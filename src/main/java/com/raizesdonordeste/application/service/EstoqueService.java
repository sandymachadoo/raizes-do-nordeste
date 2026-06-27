package com.raizesdonordeste.application.service;

import com.raizesdonordeste.api.dto.estoque.EstoqueMovimentoDTO;
import com.raizesdonordeste.api.dto.estoque.EstoqueResponseDTO;
import com.raizesdonordeste.api.exception.BusinessException;
import com.raizesdonordeste.api.exception.ResourceNotFoundException;
import com.raizesdonordeste.domain.model.Estoque;
import com.raizesdonordeste.domain.model.Produto;
import com.raizesdonordeste.domain.model.Unidade;
import com.raizesdonordeste.infrastructure.repository.EstoqueRepository;
import com.raizesdonordeste.infrastructure.repository.ProdutoRepository;
import com.raizesdonordeste.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final UnidadeService unidadeService;
    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;

    public List<EstoqueResponseDTO> listarPorUnidade(Long unidadeId) {
        unidadeService.buscarEntidade(unidadeId);
        return estoqueRepository.findByUnidadeId(unidadeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EstoqueResponseDTO registrarEntrada(EstoqueMovimentoDTO dto) {
        Estoque estoque = obterOuCriarEstoque(dto.getUnidadeId(), dto.getProdutoId());
        estoque.setQuantidade(estoque.getQuantidade() + dto.getQuantidade());
        Estoque salvo = estoqueRepository.save(estoque);

        auditoriaService.registrar(SecurityUtils.getEmailAutenticado(), "ESTOQUE_ENTRADA",
                "Unidade " + dto.getUnidadeId() + ", produto " + dto.getProdutoId()
                        + ", quantidade " + dto.getQuantidade());

        return toResponse(salvo);
    }

    @Transactional
    public EstoqueResponseDTO registrarSaida(EstoqueMovimentoDTO dto) {
        Estoque estoque = estoqueRepository
                .findByUnidadeIdAndProdutoId(dto.getUnidadeId(), dto.getProdutoId())
                .orElseThrow(() -> new BusinessException("Estoque insuficiente para o produto informado."));

        if (estoque.getQuantidade() < dto.getQuantidade()) {
            throw new BusinessException("Estoque insuficiente para o produto informado.");
        }

        estoque.setQuantidade(estoque.getQuantidade() - dto.getQuantidade());
        Estoque salvo = estoqueRepository.save(estoque);

        auditoriaService.registrar(SecurityUtils.getEmailAutenticado(), "ESTOQUE_SAIDA",
                "Unidade " + dto.getUnidadeId() + ", produto " + dto.getProdutoId()
                        + ", quantidade " + dto.getQuantidade());

        return toResponse(salvo);
    }

    public void validarDisponibilidade(Long unidadeId, Long produtoId, Integer quantidade) {
        Estoque estoque = estoqueRepository.findByUnidadeIdAndProdutoId(unidadeId, produtoId)
                .orElseThrow(() -> new BusinessException(
                        "Produto indisponível na unidade: " + produtoId));

        if (estoque.getQuantidade() < quantidade) {
            throw new BusinessException(
                    "Estoque insuficiente para o produto: " + produtoId);
        }
    }

    @Transactional
    public void restaurarEstoque(Long unidadeId, Long produtoId, Integer quantidade) {
        Estoque estoque = obterOuCriarEstoque(unidadeId, produtoId);
        estoque.setQuantidade(estoque.getQuantidade() + quantidade);
        estoqueRepository.save(estoque);
    }

    @Transactional
    public void deduzirEstoque(Long unidadeId, Long produtoId, Integer quantidade) {
        Estoque estoque = estoqueRepository.findByUnidadeIdAndProdutoId(unidadeId, produtoId)
                .orElseThrow(() -> new BusinessException("Estoque não encontrado para baixa."));

        if (estoque.getQuantidade() < quantidade) {
            throw new BusinessException("Estoque insuficiente para baixa automática.");
        }

        estoque.setQuantidade(estoque.getQuantidade() - quantidade);
        estoqueRepository.save(estoque);
    }

    private Estoque obterOuCriarEstoque(Long unidadeId, Long produtoId) {
        return estoqueRepository.findByUnidadeIdAndProdutoId(unidadeId, produtoId)
                .orElseGet(() -> {
                    Unidade unidade = unidadeService.buscarEntidade(unidadeId);
                    Produto produto = produtoRepository.findById(produtoId)
                            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado."));
                    return Estoque.builder()
                            .unidade(unidade)
                            .produto(produto)
                            .quantidade(0)
                            .build();
                });
    }

    private EstoqueResponseDTO toResponse(Estoque estoque) {
        return EstoqueResponseDTO.builder()
                .id(estoque.getId())
                .unidadeId(estoque.getUnidade().getId())
                .unidadeNome(estoque.getUnidade().getNome())
                .produtoId(estoque.getProduto().getId())
                .produtoNome(estoque.getProduto().getNome())
                .quantidade(estoque.getQuantidade())
                .build();
    }
}
