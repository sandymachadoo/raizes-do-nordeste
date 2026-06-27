package com.raizesdonordeste.api.dto.auditoria;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditoriaResponseDTO {

    private Long id;
    private String usuarioEmail;
    private String acao;
    private String detalhes;
    private LocalDateTime dataHora;
}
