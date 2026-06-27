package com.raizesdonordeste.api.dto.fidelidade;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FidelidadeResgateDTO {

    @NotNull(message = "Quantidade de pontos é obrigatória")
    @Min(value = 1, message = "Informe ao menos 1 ponto")
    private Integer pontos;
}
