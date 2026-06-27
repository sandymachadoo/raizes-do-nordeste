package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.estoque.EstoqueMovimentoDTO;
import com.raizesdonordeste.api.dto.estoque.EstoqueResponseDTO;
import com.raizesdonordeste.application.service.EstoqueService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/estoque")
@RequiredArgsConstructor
@Tag(name = "Estoque")
@SecurityRequirement(name = "bearerAuth")
public class EstoqueController {

    private final EstoqueService estoqueService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public ResponseEntity<List<EstoqueResponseDTO>> listar(@RequestParam Long unidadeId) {
        return ResponseEntity.ok(estoqueService.listarPorUnidade(unidadeId));
    }

    @PostMapping("/entrada")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<EstoqueResponseDTO> entrada(@Valid @RequestBody EstoqueMovimentoDTO dto) {
        return ResponseEntity.ok(estoqueService.registrarEntrada(dto));
    }

    @PostMapping("/saida")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<EstoqueResponseDTO> saida(@Valid @RequestBody EstoqueMovimentoDTO dto) {
        return ResponseEntity.ok(estoqueService.registrarSaida(dto));
    }
}
