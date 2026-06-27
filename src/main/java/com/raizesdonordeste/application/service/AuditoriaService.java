package com.raizesdonordeste.application.service;

import com.raizesdonordeste.api.dto.auditoria.AuditoriaResponseDTO;
import com.raizesdonordeste.domain.model.Auditoria;
import com.raizesdonordeste.infrastructure.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public void registrar(String usuarioEmail, String acao, String detalhes) {
        auditoriaRepository.save(Auditoria.builder()
                .usuarioEmail(usuarioEmail)
                .acao(acao)
                .detalhes(detalhes)
                .build());
    }

    public List<AuditoriaResponseDTO> listar() {
        return auditoriaRepository.findAll().stream()
                .map(a -> AuditoriaResponseDTO.builder()
                        .id(a.getId())
                        .usuarioEmail(a.getUsuarioEmail())
                        .acao(a.getAcao())
                        .detalhes(a.getDetalhes())
                        .dataHora(a.getDataHora())
                        .build())
                .toList();
    }
}
