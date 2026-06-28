package com.raizesdonordeste.application.service;

import com.raizesdonordeste.api.dto.pagamento.PagamentoCallbackDTO;
import com.raizesdonordeste.api.dto.pagamento.PagamentoResponseDTO;
import com.raizesdonordeste.api.exception.BusinessException;
import com.raizesdonordeste.domain.enums.FormaPagamento;
import com.raizesdonordeste.domain.enums.StatusPagamento;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.domain.model.ItemPedido;
import com.raizesdonordeste.domain.model.Pagamento;
import com.raizesdonordeste.domain.model.Pedido;
import com.raizesdonordeste.infrastructure.repository.PagamentoRepository;
import com.raizesdonordeste.infrastructure.payment.PagamentoGatewayMockClient;
import com.raizesdonordeste.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PedidoService pedidoService;
    private final EstoqueService estoqueService;
    private final FidelidadeService fidelidadeService;
    private final AuditoriaService auditoriaService;
    private final PagamentoGatewayMockClient pagamentoGatewayMockClient;

    @Transactional
    public PagamentoResponseDTO solicitarPagamento(Long pedidoId) {
        Pedido pedido = pedidoService.buscarEntidade(pedidoId);

        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new BusinessException("Pedido não está aguardando pagamento.");
        }

        pagamentoRepository.findByPedidoId(pedidoId).ifPresent(p -> {
            throw new BusinessException("Pagamento já solicitado para este pedido.");
        });

        Pagamento pagamento = Pagamento.builder()
                .pedido(pedido)
                .transactionId(UUID.randomUUID().toString())
                .status(StatusPagamento.PENDENTE)
                .formaPagamento(FormaPagamento.MOCK)
                .build();

        pagamentoGatewayMockClient.enviarSolicitacao(pedido, pagamento.getTransactionId());

        Pagamento salvo = pagamentoRepository.save(pagamento);

        auditoriaService.registrar(SecurityUtils.getEmailAutenticado(), "SOLICITAR_PAGAMENTO",
                "Pedido " + pedidoId + " transactionId " + salvo.getTransactionId());

        return toResponse(salvo);
    }

    @Transactional
    public PagamentoResponseDTO processarCallback(PagamentoCallbackDTO dto) {
        Pagamento pagamento = pagamentoRepository.findByTransactionId(dto.getTransactionId())
                .orElseThrow(() -> new BusinessException("Transação não encontrada."));

        if (pagamento.getStatus() != StatusPagamento.PENDENTE) {
            throw new BusinessException("Pagamento já processado.");
        }

        pagamento.setStatus(dto.getStatus());
        pagamento.setDataAtualizacao(LocalDateTime.now());

        Pedido pedido = pagamento.getPedido();

        if (dto.getStatus() == StatusPagamento.APROVADO) {
            pedidoService.atualizarStatusPagamento(pedido, StatusPedido.PAGO);

            for (ItemPedido item : pedido.getItens()) {
                estoqueService.deduzirEstoque(
                        pedido.getUnidade().getId(),
                        item.getProduto().getId(),
                        item.getQuantidade());
            }

            fidelidadeService.acumularPontos(pedido.getCliente(), pedido.getValorTotal());

            auditoriaService.registrar("SISTEMA", "PAGAMENTO_APROVADO",
                    "Pedido " + pedido.getId() + " transactionId " + dto.getTransactionId());

        } else if (dto.getStatus() == StatusPagamento.NEGADO) {
            pedidoService.cancelarPorPagamentoNegado(pedido);
        } else {
            throw new BusinessException("Status de callback inválido.");
        }

        Pagamento salvo = pagamentoRepository.save(pagamento);
        return toResponse(salvo);
    }

    private PagamentoResponseDTO toResponse(Pagamento pagamento) {
        return PagamentoResponseDTO.builder()
                .id(pagamento.getId())
                .pedidoId(pagamento.getPedido().getId())
                .transactionId(pagamento.getTransactionId())
                .status(pagamento.getStatus().name())
                .build();
    }
}
