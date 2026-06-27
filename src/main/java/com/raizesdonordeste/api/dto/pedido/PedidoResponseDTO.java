package com.raizesdonordeste.api.dto.pedido;

import com.raizesdonordeste.domain.enums.CanalPedido;
import com.raizesdonordeste.domain.enums.StatusPedido;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PedidoResponseDTO {

    private Long id;
    private Long clienteId;
    private String clienteNome;
    private Long unidadeId;
    private String unidadeNome;
    private CanalPedido canalPedido;
    private StatusPedido status;
    private BigDecimal valorTotal;
    private Integer pontosResgatados;
    private LocalDateTime dataCriacao;
    private List<ItemPedidoResponseDTO> itens;
}
