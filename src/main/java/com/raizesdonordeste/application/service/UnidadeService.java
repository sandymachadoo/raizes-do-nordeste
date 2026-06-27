package com.raizesdonordeste.application.service;

import com.raizesdonordeste.api.dto.unidade.UnidadeRequestDTO;
import com.raizesdonordeste.api.dto.unidade.UnidadeResponseDTO;
import com.raizesdonordeste.api.exception.ResourceNotFoundException;
import com.raizesdonordeste.domain.model.Unidade;
import com.raizesdonordeste.infrastructure.repository.UnidadeRepository;
import com.raizesdonordeste.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final AuditoriaService auditoriaService;

    public List<UnidadeResponseDTO> listarUnidades() {
        return unidadeRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UnidadeResponseDTO buscarPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    public Unidade buscarEntidade(Long id) {
        return unidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada."));
    }

    @Transactional
    public UnidadeResponseDTO criarUnidade(UnidadeRequestDTO dto) {
        Unidade unidade = Unidade.builder()
                .nome(dto.getNome())
                .endereco(dto.getEndereco())
                .cidade(dto.getCidade())
                .estado(dto.getEstado())
                .ativa(dto.getAtiva() == null || dto.getAtiva())
                .build();

        Unidade salva = unidadeRepository.save(unidade);

        auditoriaService.registrar(SecurityUtils.getEmailAutenticado(), "CRIAR_UNIDADE",
                "Unidade criada: " + salva.getNome());

        return toResponse(salva);
    }

    @Transactional
    public UnidadeResponseDTO atualizarUnidade(Long id, UnidadeRequestDTO dto) {
        Unidade unidade = buscarEntidade(id);
        unidade.setNome(dto.getNome());
        unidade.setEndereco(dto.getEndereco());
        unidade.setCidade(dto.getCidade());
        unidade.setEstado(dto.getEstado());

        if (dto.getAtiva() != null) {
            unidade.setAtiva(dto.getAtiva());
        }

        Unidade atualizada = unidadeRepository.save(unidade);

        auditoriaService.registrar(SecurityUtils.getEmailAutenticado(), "ATUALIZAR_UNIDADE",
                "Unidade atualizada: " + atualizada.getNome());

        return toResponse(atualizada);
    }

    private UnidadeResponseDTO toResponse(Unidade unidade) {
        return UnidadeResponseDTO.builder()
                .id(unidade.getId())
                .nome(unidade.getNome())
                .endereco(unidade.getEndereco())
                .cidade(unidade.getCidade())
                .estado(unidade.getEstado())
                .ativa(unidade.getAtiva())
                .build();
    }
}
