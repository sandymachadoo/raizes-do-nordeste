package com.raizesdonordeste.infrastructure.repository;

import com.raizesdonordeste.domain.model.Fidelidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FidelidadeRepository extends JpaRepository<Fidelidade, Long> {

    Optional<Fidelidade> findByUsuarioId(Long usuarioId);
}
