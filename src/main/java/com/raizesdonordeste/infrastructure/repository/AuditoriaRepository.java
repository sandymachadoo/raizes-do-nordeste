package com.raizesdonordeste.infrastructure.repository;

import com.raizesdonordeste.domain.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
}
