package com.raizesdonordeste.api.dto.pagamento;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PagamentoResponseDTO {

    private Long id;
    private Long pedidoId;
    private String transactionId;
    private String status;
}
