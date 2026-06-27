package com.raizesdonordeste.application.service;

import com.raizesdonordeste.api.dto.pedido.*;
import com.raizesdonordeste.api.exception.BusinessException;
import com.raizesdonordeste.api.exception.ResourceNotFoundException;
import com.raizesdonordeste.domain.enums.CanalPedido;
import com.raizesdonordeste.domain.enums.Role;
import com.raizesdonordeste.domain.enums.StatusPagamento;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.domain.model.*;
import com.raizesdonordeste.infrastructure.repository.PedidoRepository;
import com.raizesdonordeste.infrastructure.specification.PedidoSpecification;
import com.raizesdonordeste.infrastructure.repository.PagamentoRepository;
import com.raizesdonordeste.infrastructure.repository.ProdutoRepository;
import com.raizesdonordeste.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final Map<StatusPedido, Set<StatusPedido>> TRANSICOES_PERMITIDAS = Map.of(
            StatusPedido.PAGO, Set.of(StatusPedido.EM_PREPARO),
            StatusPedido.EM_PREPARO, Set.of(StatusPedido.PRONTO),
            StatusPedido.PRONTO, Set.of(StatusPedido.ENTREGUE)
    );

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ProdutoRepository produtoRepository;
    private final UnidadeService unidadeService;
    private final EstoqueService estoqueService;
    private final UsuarioService usuarioService;
    private final FidelidadeService fidelidadeService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public PedidoResponseDTO criarPedido(PedidoRequestDTO dto) {
        Usuario cliente = usuarioService.buscarEntidadePorEmail(SecurityUtils.getEmailAutenticado());

        if (cliente.getRole() != Role.CLIENTE) {
            throw new BusinessException("Apenas clientes podem criar pedidos.");
        }

        Unidade unidade = unidadeService.buscarEntidade(dto.getUnidadeId());
        if (!Boolean.TRUE.equals(unidade.getAtiva())) {
            throw new BusinessException("Unidade inativa não pode receber pedidos.");
        }

        if (dto.getCanalPedido() == null) {
            throw new BusinessException("Canal do pedido é obrigatório.");
        }

        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal valorTotal = BigDecimal.ZERO;

        for (ItemPedidoRequestDTO itemDto : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produto não encontrado: " + itemDto.getProdutoId()));

            if (!Boolean.TRUE.equals(produto.getAtivo())) {
                throw new BusinessException("Produto inativo: " + produto.getNome());
            }

            estoqueService.validarDisponibilidade(unidade.getId(), produto.getId(), itemDto.getQuantidade());

            BigDecimal subtotal = produto.getPreco().multiply(BigDecimal.valueOf(itemDto.getQuantidade()));
            valorTotal = valorTotal.add(subtotal);

            ItemPedido item = ItemPedido.builder()
                    .produto(produto)
                    .quantidade(itemDto.getQuantidade())
                    .precoUnitario(produto.getPreco())
                    .subtotal(subtotal)
                    .build();
            itens.add(item);
        }

        int pontosResgatados = dto.getPontosResgatados() != null ? dto.getPontosResgatados() : 0;
        if (pontosResgatados > 0) {
            BigDecimal desconto = fidelidadeService.aplicarResgate(cliente, pontosResgatados);
            valorTotal = valorTotal.subtract(desconto).max(BigDecimal.ZERO);
        }

        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .unidade(unidade)
                .canalPedido(dto.getCanalPedido())
                .status(StatusPedido.AGUARDANDO_PAGAMENTO)
                .valorTotal(valorTotal)
                .pontosResgatados(pontosResgatados)
                .build();

        for (ItemPedido item : itens) {
            item.setPedido(pedido);
            pedido.getItens().add(item);
        }

        Pedido salvo = pedidoRepository.save(pedido);

        auditoriaService.registrar(cliente.getEmail(), "CRIAR_PEDIDO",
                "Pedido " + salvo.getId() + " canal " + salvo.getCanalPedido());

        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPedidos(CanalPedido canalPedido, StatusPedido status, Long unidadeId) {
        Usuario usuario = usuarioService.buscarEntidadePorEmail(SecurityUtils.getEmailAutenticado());

        List<Pedido> pedidos = pedidoRepository.findAll(
                PedidoSpecification.comFiltros(canalPedido, status, unidadeId));

        if (usuario.getRole() == Role.CLIENTE) {
            pedidos = pedidos.stream()
                    .filter(p -> p.getCliente().getId().equals(usuario.getId()))
                    .toList();
        }

        return pedidos.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO buscarPorId(Long id) {
        Pedido pedido = buscarEntidade(id);
        validarAcesso(pedido);
        return toResponse(pedido);
    }

    public Pedido buscarEntidade(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));
    }

    @Transactional
    public PedidoResponseDTO atualizarStatus(Long id, StatusPedido novoStatus) {
        Pedido pedido = buscarEntidade(id);
        Usuario usuario = usuarioService.buscarEntidadePorEmail(SecurityUtils.getEmailAutenticado());

        validarPermissaoTransicao(usuario.getRole(), pedido.getStatus(), novoStatus);

        if (!TRANSICOES_PERMITIDAS.containsKey(pedido.getStatus())
                || !TRANSICOES_PERMITIDAS.get(pedido.getStatus()).contains(novoStatus)) {
            throw new BusinessException("Transição de status não permitida.");
        }

        pedido.setStatus(novoStatus);
        Pedido atualizado = pedidoRepository.save(pedido);

        auditoriaService.registrar(SecurityUtils.getEmailAutenticado(), "ATUALIZAR_STATUS_PEDIDO",
                "Pedido " + id + " status " + novoStatus);

        return toResponse(atualizado);
    }

    @Transactional
    public PedidoResponseDTO cancelarPedido(Long id) {
        Pedido pedido = buscarEntidade(id);
        Usuario usuario = usuarioService.buscarEntidadePorEmail(SecurityUtils.getEmailAutenticado());
        validarPermissaoCancelamento(pedido, usuario);
        executarCancelamento(pedido, usuario.getEmail(), "CANCELAR_PEDIDO");
        return toResponse(pedido);
    }

    @Transactional
    public void cancelarPorPagamentoNegado(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            return;
        }
        executarCancelamento(pedido, "SISTEMA", "PAGAMENTO_NEGADO");
    }

    @Transactional
    public void atualizarStatusPagamento(Pedido pedido, StatusPedido novoStatus) {
        pedido.setStatus(novoStatus);
        pedidoRepository.save(pedido);
    }

    private void validarPermissaoTransicao(Role role, StatusPedido atual, StatusPedido novo) {
        if (atual == StatusPedido.PAGO && novo == StatusPedido.EM_PREPARO) {
            if (role != Role.ATENDENTE && role != Role.GERENTE && role != Role.ADMIN) {
                throw new BusinessException("Sem permissão para esta transição.");
            }
        } else if (atual == StatusPedido.EM_PREPARO && novo == StatusPedido.PRONTO) {
            if (role != Role.COZINHA && role != Role.GERENTE && role != Role.ADMIN) {
                throw new BusinessException("Sem permissão para esta transição.");
            }
        } else if (atual == StatusPedido.PRONTO && novo == StatusPedido.ENTREGUE) {
            if (role != Role.ATENDENTE && role != Role.GERENTE && role != Role.ADMIN) {
                throw new BusinessException("Sem permissão para esta transição.");
            }
        }
    }

    private void validarAcesso(Pedido pedido) {
        Usuario usuario = usuarioService.buscarEntidadePorEmail(SecurityUtils.getEmailAutenticado());
        if (usuario.getRole() == Role.CLIENTE
                && !pedido.getCliente().getId().equals(usuario.getId())) {
            throw new BusinessException("Acesso negado ao pedido.");
        }
    }

    private void validarPermissaoCancelamento(Pedido pedido, Usuario usuario) {
        if (pedido.getStatus() == StatusPedido.CANCELADO || pedido.getStatus() == StatusPedido.ENTREGUE) {
            throw new BusinessException("Pedido não pode ser cancelado.");
        }

        if (pedido.getStatus() == StatusPedido.PRONTO) {
            throw new BusinessException("Pedido pronto não pode ser cancelado.");
        }

        if (usuario.getRole() == Role.CLIENTE) {
            if (!pedido.getCliente().getId().equals(usuario.getId())) {
                throw new BusinessException("Acesso negado ao pedido.");
            }
            if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
                throw new BusinessException("Cliente só pode cancelar pedidos aguardando pagamento.");
            }
            return;
        }

        if (usuario.getRole() == Role.ATENDENTE || usuario.getRole() == Role.GERENTE
                || usuario.getRole() == Role.ADMIN) {
            if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO
                    && pedido.getStatus() != StatusPedido.PAGO
                    && pedido.getStatus() != StatusPedido.EM_PREPARO) {
                throw new BusinessException("Status do pedido não permite cancelamento.");
            }
            return;
        }

        throw new BusinessException("Sem permissão para cancelar pedido.");
    }

    private void executarCancelamento(Pedido pedido, String emailAuditoria, String acaoAuditoria) {
        boolean estoqueFoiDeduzido = pedido.getStatus() == StatusPedido.PAGO
                || pedido.getStatus() == StatusPedido.EM_PREPARO;

        if (estoqueFoiDeduzido) {
            for (ItemPedido item : pedido.getItens()) {
                estoqueService.restaurarEstoque(
                        pedido.getUnidade().getId(),
                        item.getProduto().getId(),
                        item.getQuantidade());
            }
        }

        if (pedido.getPontosResgatados() != null && pedido.getPontosResgatados() > 0) {
            fidelidadeService.estornarResgate(pedido.getCliente(), pedido.getPontosResgatados());
        }

        pagamentoRepository.findByPedidoId(pedido.getId()).ifPresent(pagamento -> {
            if (pagamento.getStatus() == StatusPagamento.PENDENTE) {
                pagamento.setStatus(StatusPagamento.NEGADO);
                pagamento.setDataAtualizacao(LocalDateTime.now());
                pagamentoRepository.save(pagamento);
            }
        });

        pedido.setStatus(StatusPedido.CANCELADO);
        pedidoRepository.save(pedido);

        auditoriaService.registrar(emailAuditoria, acaoAuditoria,
                "Pedido " + pedido.getId() + " cancelado");
    }

    private PedidoResponseDTO toResponse(Pedido pedido) {
        List<ItemPedidoResponseDTO> itens = pedido.getItens().stream()
                .map(item -> ItemPedidoResponseDTO.builder()
                        .produtoId(item.getProduto().getId())
                        .produtoNome(item.getProduto().getNome())
                        .quantidade(item.getQuantidade())
                        .precoUnitario(item.getPrecoUnitario())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        return PedidoResponseDTO.builder()
                .id(pedido.getId())
                .clienteId(pedido.getCliente().getId())
                .clienteNome(pedido.getCliente().getNome())
                .unidadeId(pedido.getUnidade().getId())
                .unidadeNome(pedido.getUnidade().getNome())
                .canalPedido(pedido.getCanalPedido())
                .status(pedido.getStatus())
                .valorTotal(pedido.getValorTotal())
                .pontosResgatados(pedido.getPontosResgatados())
                .dataCriacao(pedido.getDataCriacao())
                .itens(itens)
                .build();
    }
}
