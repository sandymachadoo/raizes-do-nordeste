package com.raizesdonordeste.application.service;

import com.raizesdonordeste.domain.model.Auditoria;
import com.raizesdonordeste.infrastructure.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
