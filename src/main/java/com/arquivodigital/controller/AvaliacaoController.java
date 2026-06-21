package com.arquivodigital.controller;

import com.arquivodigital.dto.request.AvaliacaoRequest;
import com.arquivodigital.dto.response.AvaliacaoResponse;
import com.arquivodigital.security.UserDetailsImpl;
import com.arquivodigital.service.AvaliacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/avaliacoes")
@RequiredArgsConstructor
@Tag(name = "Avaliações", description = "Gosto / Não gosto em documentários")
@SecurityRequirement(name = "bearerAuth")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @GetMapping("/{docId}")
    @Operation(summary = "Obter a minha avaliação de um documentário")
    public ResponseEntity<AvaliacaoResponse> obter(
            @PathVariable Long docId,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(avaliacaoService.obter(docId, principal.getUtilizador()));
    }

    @PostMapping("/{docId}")
    @Operation(summary = "Avaliar um documentário (1=gosto, -1=não gosto)")
    public ResponseEntity<AvaliacaoResponse> avaliar(
            @PathVariable Long docId,
            @Valid @RequestBody AvaliacaoRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(avaliacaoService.avaliar(docId, request, principal.getUtilizador()));
    }

    @DeleteMapping("/{docId}")
    @Operation(summary = "Remover avaliação de um documentário")
    public ResponseEntity<Void> remover(
            @PathVariable Long docId,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        avaliacaoService.remover(docId, principal.getUtilizador());
        return ResponseEntity.noContent().build();
    }
}
