package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.auth.LoginRequestDTO;
import com.raizesdonordeste.api.dto.auth.LoginResponseDTO;
import com.raizesdonordeste.api.dto.auth.RegistroClienteRequestDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioResponseDTO;
import com.raizesdonordeste.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    @Operation(summary = "Cadastro de cliente com consentimento LGPD")
    public ResponseEntity<UsuarioResponseDTO> registrarCliente(@Valid @RequestBody RegistroClienteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarCliente(dto));
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticação por e-mail e senha")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }
}
