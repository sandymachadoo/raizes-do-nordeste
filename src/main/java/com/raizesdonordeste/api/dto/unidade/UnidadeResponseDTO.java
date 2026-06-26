package com.raizesdonordeste.api.dto.unidade;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnidadeResponseDTO {

    private Long id;
    private String nome;
    private String endereco;
    private String cidade;
    private String estado;
    private Boolean ativa;
}
