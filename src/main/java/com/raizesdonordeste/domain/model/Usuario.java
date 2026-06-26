package com.raizesdonordeste.domain.model;

import com.raizesdonordeste.domain.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @Email
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    private String senha;

    private String telefone;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder.Default
    private Boolean ativo = true;
}