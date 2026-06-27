package com.raizesdonordeste.infrastructure.specification;

import com.raizesdonordeste.domain.enums.CanalPedido;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.domain.model.Pedido;
import org.springframework.data.jpa.domain.Specification;

public class PedidoSpecification {

    private PedidoSpecification() {
    }

    public static Specification<Pedido> comFiltros(CanalPedido canalPedido, StatusPedido status, Long unidadeId) {
        return porCanal(canalPedido)
                .and(porStatus(status))
                .and(porUnidade(unidadeId));
    }

    private static Specification<Pedido> porCanal(CanalPedido canalPedido) {
        return (root, query, cb) -> canalPedido == null ? null : cb.equal(root.get("canalPedido"), canalPedido);
    }

    private static Specification<Pedido> porStatus(StatusPedido status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<Pedido> porUnidade(Long unidadeId) {
        return (root, query, cb) -> unidadeId == null ? null : cb.equal(root.get("unidade").get("id"), unidadeId);
    }
}
