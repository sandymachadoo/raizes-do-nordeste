package com.raizesdonordeste.domain.model;

import com.raizesdonordeste.domain.enums.TipoMovimentoFidelidade;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimentos_fidelidade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentoFidelidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fidelidade_id", nullable = false)
    private Fidelidade fidelidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentoFidelidade tipo;

    @Column(nullable = false)
    private Integer pontos;

    private String descricao;

    @Builder.Default
    private LocalDateTime dataMovimento = LocalDateTime.now();
}
