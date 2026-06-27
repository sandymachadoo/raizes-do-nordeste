package com.raizesdonordeste.application.service;

import com.raizesdonordeste.api.dto.produto.ProdutoRequestDTO;
import com.raizesdonordeste.api.dto.produto.ProdutoResponseDTO;
import com.raizesdonordeste.api.exception.BusinessException;
import com.raizesdonordeste.api.exception.ResourceNotFoundException;
import com.raizesdonordeste.domain.model.Estoque;
import com.raizesdonordeste.domain.model.Produto;
import com.raizesdonordeste.infrastructure.repository.EstoqueRepository;
import com.raizesdonordeste.infrastructure.repository.ProdutoRepository;
import com.raizesdonordeste.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public ProdutoResponseDTO criarProduto(ProdutoRequestDTO dto) {

        if (dto.getNome() == null || dto.getNome().isBlank()) {
            throw new BusinessException("Nome do produto é obrigatório.");
        }

        Produto produto = Produto.builder()
                .nome(dto.getNome())
                .descricao(dto.getDescricao())
                .preco(dto.getPreco())
                .ativo(dto.getAtivo() == null ? true : dto.getAtivo())
                .build();

        Produto salvo = produtoRepository.save(produto);

        auditoriaService.registrar(
                SecurityUtils.getEmailAutenticado(),
                "CRIAR_PRODUTO",
                "Produto criado: " + salvo.getNome());

        return toResponse(salvo, null);
    }

    public List<ProdutoResponseDTO> listarProdutos(Long unidadeId) {
        return produtoRepository.findByAtivoTrue()
                .stream()
                .map(produto -> toResponse(produto, unidadeId))
                .toList();
    }

    public ProdutoResponseDTO buscarPorId(Long id, Long unidadeId) {

        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Produto não encontrado."));

        return toResponse(produto, unidadeId);
    }

    @Transactional
    public ProdutoResponseDTO atualizarProduto(Long id, ProdutoRequestDTO dto) {

        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Produto não encontrado."));

        produto.setNome(dto.getNome());
        produto.setDescricao(dto.getDescricao());
        produto.setPreco(dto.getPreco());

        if (dto.getAtivo() != null) {
            produto.setAtivo(dto.getAtivo());
        }

        Produto atualizado = produtoRepository.save(produto);

        auditoriaService.registrar(
                SecurityUtils.getEmailAutenticado(),
                "ATUALIZAR_PRODUTO",
                "Produto atualizado: " + atualizado.getNome());

        return toResponse(atualizado, null);
    }

    @Transactional
    public void desativarProduto(Long id) {

        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Produto não encontrado."));

        produto.setAtivo(false);

        produtoRepository.save(produto);

        auditoriaService.registrar(
                SecurityUtils.getEmailAutenticado(),
                "DESATIVAR_PRODUTO",
                "Produto desativado: " + produto.getNome());
    }

    private ProdutoResponseDTO toResponse(Produto produto, Long unidadeId) {

        Integer quantidade = null;
        if (unidadeId != null) {
            quantidade = estoqueRepository.findByUnidadeIdAndProdutoId(unidadeId, produto.getId())
                    .map(Estoque::getQuantidade)
                    .orElse(0);
        }

        return ProdutoResponseDTO.builder()
                .id(produto.getId())
                .nome(produto.getNome())
                .descricao(produto.getDescricao())
                .preco(produto.getPreco())
                .ativo(produto.getAtivo())
                .quantidadeDisponivel(quantidade)
                .build();
    }
}