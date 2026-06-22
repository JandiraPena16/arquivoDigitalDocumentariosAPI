package com.arquivodigital.controller;

import com.arquivodigital.dto.response.LiveResponse;
import com.arquivodigital.exception.custom.ResourceNotFoundException;
import com.arquivodigital.signaling.LiveRegistry;
import com.arquivodigital.signaling.LiveSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/lives")
@RequiredArgsConstructor
@Tag(name = "Lives", description = "Transmissões ao vivo (WebRTC) activas")
public class LiveController {

    private final LiveRegistry registry;

    @GetMapping
    @Operation(summary = "Listar transmissões ao vivo activas (público)")
    public ResponseEntity<List<LiveResponse>> listar() {
        List<LiveResponse> lives = registry.activas().stream()
                .sorted(Comparator.comparing(LiveSession::getIniciadaEm).reversed())
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(lives);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes de uma live activa")
    public ResponseEntity<LiveResponse> buscar(@PathVariable String id) {
        LiveSession live = registry.porId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Live não encontrada ou já terminada: " + id));
        return ResponseEntity.ok(toResponse(live));
    }

    private LiveResponse toResponse(LiveSession s) {
        return LiveResponse.builder()
                .id(s.getLiveId())
                .titulo(s.getTitulo())
                .nomeBroadcaster(s.getBroadcasterNome())
                .broadcasterUserId(s.getBroadcasterUserId())
                .numEspectadores(s.getNumEspectadores())
                .iniciadaEm(s.getIniciadaEm())
                .build();
    }
}
