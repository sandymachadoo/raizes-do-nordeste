package com.raizesdonordeste.infrastructure.repository;

import com.raizesdonordeste.domain.model.MovimentoFidelidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentoFidelidadeRepository extends JpaRepository<MovimentoFidelidade, Long> {

    List<MovimentoFidelidade> findByFidelidadeIdOrderByDataMovimentoDesc(Long fidelidadeId);
}
