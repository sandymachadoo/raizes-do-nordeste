package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.pagamento.PagamentoCallbackDTO;
import com.raizesdonordeste.api.dto.pagamento.PagamentoResponseDTO;
import com.raizesdonordeste.application.service.PagamentoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
@Tag(name = "Pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping("/solicitar/{pedidoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagamentoResponseDTO> solicitar(@PathVariable Long pedidoId) {
        return ResponseEntity.ok(pagamentoService.solicitarPagamento(pedidoId));
    }

    @PostMapping("/callback")
    public ResponseEntity<PagamentoResponseDTO> callback(@Valid @RequestBody PagamentoCallbackDTO dto) {
        return ResponseEntity.ok(pagamentoService.processarCallback(dto));
    }
}
