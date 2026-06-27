package com.raizesdonordeste.api.dto.fidelidade;

import com.raizesdonordeste.domain.enums.TipoMovimentoFidelidade;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MovimentoFidelidadeResponseDTO {

    private TipoMovimentoFidelidade tipo;
    private Integer pontos;
    private String descricao;
    private LocalDateTime dataMovimento;
}
