package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.unidade.UnidadeRequestDTO;
import com.raizesdonordeste.api.dto.unidade.UnidadeResponseDTO;
import com.raizesdonordeste.application.service.UnidadeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/unidades")
@RequiredArgsConstructor
@Tag(name = "Unidades")
public class UnidadeController {

    private final UnidadeService unidadeService;

    @GetMapping
    public ResponseEntity<List<UnidadeResponseDTO>> listar() {
        return ResponseEntity.ok(unidadeService.listarUnidades());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadeResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(unidadeService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UnidadeResponseDTO> criar(@Valid @RequestBody UnidadeRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unidadeService.criarUnidade(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UnidadeResponseDTO> atualizar(@PathVariable Long id,
                                                          @Valid @RequestBody UnidadeRequestDTO dto) {
        return ResponseEntity.ok(unidadeService.atualizarUnidade(id, dto));
    }
}
