package com.raizesdonordeste.api.dto.estoque;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EstoqueResponseDTO {

    private Long id;
    private Long unidadeId;
    private String unidadeNome;
    private Long produtoId;
    private String produtoNome;
    private Integer quantidade;
}
