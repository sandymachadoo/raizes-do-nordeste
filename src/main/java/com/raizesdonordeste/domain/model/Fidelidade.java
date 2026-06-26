package com.raizesdonordeste.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fidelidades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fidelidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    @Builder.Default
    private Integer saldoPontos = 0;
}
