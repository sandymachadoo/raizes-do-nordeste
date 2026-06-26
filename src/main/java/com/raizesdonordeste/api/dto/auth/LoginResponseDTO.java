package com.raizesdonordeste.api.dto.auth;

import com.raizesdonordeste.domain.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDTO {

    private String token;
    private String email;
    private Role role;
}
