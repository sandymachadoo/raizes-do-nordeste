package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.fidelidade.FidelidadeResgateDTO;
import com.raizesdonordeste.api.dto.fidelidade.MovimentoFidelidadeResponseDTO;
import com.raizesdonordeste.api.dto.fidelidade.SaldoFidelidadeResponseDTO;
import com.raizesdonordeste.application.service.FidelidadeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fidelidade")
@RequiredArgsConstructor
@Tag(name = "Fidelidade")
@SecurityRequirement(name = "bearerAuth")
public class FidelidadeController {

    private final FidelidadeService fidelidadeService;

    @GetMapping("/saldo")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<SaldoFidelidadeResponseDTO> saldo() {
        return ResponseEntity.ok(fidelidadeService.consultarSaldo());
    }

    @GetMapping("/historico")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<MovimentoFidelidadeResponseDTO>> historico() {
        return ResponseEntity.ok(fidelidadeService.consultarHistorico());
    }

    @PostMapping("/resgate")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<SaldoFidelidadeResponseDTO> resgate(@Valid @RequestBody FidelidadeResgateDTO dto) {
        return ResponseEntity.ok(fidelidadeService.resgatarPontos(dto));
    }
}
