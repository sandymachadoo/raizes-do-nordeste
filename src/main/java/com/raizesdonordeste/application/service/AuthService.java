package com.raizesdonordeste.application.service;

import com.raizesdonordeste.api.dto.auth.LoginRequestDTO;
import com.raizesdonordeste.api.dto.auth.LoginResponseDTO;
import com.raizesdonordeste.api.dto.auth.RegistroClienteRequestDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioResponseDTO;
import com.raizesdonordeste.domain.enums.Role;
import com.raizesdonordeste.domain.model.Fidelidade;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.infrastructure.repository.FidelidadeRepository;
import com.raizesdonordeste.infrastructure.repository.UsuarioRepository;
import com.raizesdonordeste.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final FidelidadeRepository fidelidadeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public UsuarioResponseDTO registrarCliente(RegistroClienteRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senhaHash(passwordEncoder.encode(dto.getSenha()))
                .telefone(dto.getTelefone())
                .role(Role.CLIENTE)
                .ativo(true)
                .consentimentoLgpd(true)
                .dataConsentimentoLgpd(LocalDateTime.now())
                .build();

        Usuario salvo = usuarioRepository.save(usuario);

        fidelidadeRepository.save(Fidelidade.builder()
                .usuario(salvo)
                .saldoPontos(0)
                .build());

        auditoriaService.registrar(salvo.getEmail(), "REGISTRO_CLIENTE",
                "Cliente registrado com consentimento LGPD.");

        return toResponse(salvo);
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getSenha()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(dto.getEmail());
        String token = jwtService.generateToken(userDetails);

        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        auditoriaService.registrar(usuario.getEmail(), "LOGIN", "Autenticação realizada com sucesso.");

        return LoginResponseDTO.builder()
                .token(token)
                .email(usuario.getEmail())
                .role(usuario.getRole())
                .build();
    }

    private UsuarioResponseDTO toResponse(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .telefone(usuario.getTelefone())
                .role(usuario.getRole())
                .ativo(usuario.getAtivo())
                .consentimentoLgpd(usuario.getConsentimentoLgpd())
                .dataConsentimentoLgpd(usuario.getDataConsentimentoLgpd())
                .build();
    }
}
