package com.raizesdonordeste.api.dto.unidade;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnidadeRequestDTO {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    private String endereco;
    private String cidade;
    private String estado;
    private Boolean ativa;
}
