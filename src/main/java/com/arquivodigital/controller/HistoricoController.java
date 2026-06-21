package com.arquivodigital.controller;

import com.arquivodigital.dto.response.DocumentarioResponse;
import com.arquivodigital.security.UserDetailsImpl;
import com.arquivodigital.service.HistoricoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historico")
@RequiredArgsConstructor
@Tag(name = "Histórico", description = "Histórico de documentários vistos")
@SecurityRequirement(name = "bearerAuth")
public class HistoricoController {

    private final HistoricoService historicoService;

    @GetMapping
    @Operation(summary = "Listar o meu histórico de visualizações")
    public ResponseEntity<List<DocumentarioResponse>> listar(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(historicoService.listar(principal.getUtilizador()));
    }

    @PostMapping("/{docId}")
    @Operation(summary = "Registar visualização de um documentário")
    public ResponseEntity<Void> registar(
            @PathVariable Long docId,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        historicoService.registar(docId, principal.getUtilizador());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @Operation(summary = "Limpar o meu histórico")
    public ResponseEntity<Void> limpar(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        historicoService.limpar(principal.getUtilizador());
        return ResponseEntity.noContent().build();
    }
}
