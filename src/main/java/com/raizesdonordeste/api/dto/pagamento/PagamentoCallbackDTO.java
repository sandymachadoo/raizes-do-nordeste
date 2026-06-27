package com.raizesdonordeste.api.dto.pagamento;

import com.raizesdonordeste.domain.enums.StatusPagamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PagamentoCallbackDTO {

    @NotBlank(message = "Transaction ID é obrigatório")
    private String transactionId;

    @NotNull(message = "Status é obrigatório")
    private StatusPagamento status;
}
