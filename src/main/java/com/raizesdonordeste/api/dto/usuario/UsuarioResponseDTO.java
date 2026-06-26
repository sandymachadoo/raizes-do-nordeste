package com.raizesdonordeste.api.dto.usuario;

import com.raizesdonordeste.domain.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponseDTO {

    private Long id;

    private String nome;

    private String email;

    private String telefone;

    private Role role;

    private Boolean ativo;
}