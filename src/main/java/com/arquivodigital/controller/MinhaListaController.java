package com.arquivodigital.controller;

import com.arquivodigital.dto.response.DocumentarioResponse;
import com.arquivodigital.security.UserDetailsImpl;
import com.arquivodigital.service.MinhaListaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lista")
@RequiredArgsConstructor
@Tag(name = "Minha Lista", description = "Gestão da lista de documentários para ver")
@SecurityRequirement(name = "bearerAuth")
public class MinhaListaController {

    private final MinhaListaService minhaListaService;

    @GetMapping
    @Operation(summary = "Listar os documentários da minha lista")
    public ResponseEntity<List<DocumentarioResponse>> listar(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(minhaListaService.listar(principal.getUtilizador()));
    }

    @PostMapping("/{docId}")
    @Operation(summary = "Adicionar documentário à minha lista")
    public ResponseEntity<Void> adicionar(
            @PathVariable Long docId,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        minhaListaService.adicionar(docId, principal.getUtilizador());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{docId}")
    @Operation(summary = "Remover documentário da minha lista")
    public ResponseEntity<Void> remover(
            @PathVariable Long docId,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        minhaListaService.remover(docId, principal.getUtilizador());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{docId}/status")
    @Operation(summary = "Verificar se documentário está na minha lista")
    public ResponseEntity<Map<String, Boolean>> verificar(
            @PathVariable Long docId,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        boolean esta = minhaListaService.verificar(docId, principal.getUtilizador());
        return ResponseEntity.ok(Map.of("naLista", esta));
    }
}
