package com.raizesdonordeste.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuarioEmail;

    @Column(nullable = false)
    private String acao;

    @Column(columnDefinition = "TEXT")
    private String detalhes;

    @Builder.Default
    private LocalDateTime dataHora = LocalDateTime.now();
}
