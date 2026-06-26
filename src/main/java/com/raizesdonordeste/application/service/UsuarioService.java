package com.raizesdonordeste.application.service;

import com.raizesdonordeste.api.dto.usuario.UsuarioRequestDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioResponseDTO;
import com.raizesdonordeste.api.exception.ResourceNotFoundException;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.infrastructure.repository.UsuarioRepository;
import com.raizesdonordeste.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioResponseDTO criarUsuario(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senhaHash(passwordEncoder.encode(dto.getSenha()))
                .telefone(dto.getTelefone())
                .role(dto.getRole())
                .ativo(true)
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        auditoriaService.registrar(SecurityUtils.getEmailAutenticado(), "CRIAR_USUARIO",
                "Usuário criado: " + usuarioSalvo.getEmail() + " perfil " + usuarioSalvo.getRole());

        return toResponse(usuarioSalvo);
    }

    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        return toResponse(usuario);
    }

    public Usuario buscarEntidadePorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
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
