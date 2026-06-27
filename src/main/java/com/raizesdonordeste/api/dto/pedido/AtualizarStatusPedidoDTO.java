package com.raizesdonordeste.api.dto.pedido;

import com.raizesdonordeste.domain.enums.StatusPedido;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtualizarStatusPedidoDTO {

    @NotNull(message = "Status é obrigatório")
    private StatusPedido status;
}
