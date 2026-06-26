package com.raizesdonordeste.infrastructure.repository;

import com.raizesdonordeste.domain.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Optional<Pagamento> findByTransactionId(String transactionId);

    Optional<Pagamento> findByPedidoId(Long pedidoId);
}
