package com.raizesdonordeste.application.service;

import com.raizesdonordeste.api.dto.usuario.UsuarioRequestDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioResponseDTO;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.infrastructure.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioResponseDTO criarUsuario(UsuarioRequestDTO dto) {

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senha(passwordEncoder.encode(dto.getSenha()))
                .telefone(dto.getTelefone())
                .role(dto.getRole())
                .ativo(true)
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        return UsuarioResponseDTO.builder()
                .id(usuarioSalvo.getId())
                .nome(usuarioSalvo.getNome())
                .email(usuarioSalvo.getEmail())
                .telefone(usuarioSalvo.getTelefone())
                .role(usuarioSalvo.getRole())
                .ativo(usuarioSalvo.getAtivo())
                .build();
    }

    public List<UsuarioResponseDTO> listarUsuarios() {

        return usuarioRepository.findAll()
                .stream()
                .map(usuario -> UsuarioResponseDTO.builder()
                        .id(usuario.getId())
                        .nome(usuario.getNome())
                        .email(usuario.getEmail())
                        .telefone(usuario.getTelefone())
                        .role(usuario.getRole())
                        .ativo(usuario.getAtivo())
                        .build())
                .toList();
    }

    public UsuarioResponseDTO buscarPorId(Long id) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .telefone(usuario.getTelefone())
                .role(usuario.getRole())
                .ativo(usuario.getAtivo())
                .build();
    }
}