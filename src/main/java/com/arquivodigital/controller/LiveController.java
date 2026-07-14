package com.arquivodigital.controller;

import com.arquivodigital.dto.response.LiveResponse;
import com.arquivodigital.entity.AcaoLog;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.exception.custom.ResourceNotFoundException;
import com.arquivodigital.security.UserDetailsImpl;
import com.arquivodigital.service.LogService;
import com.arquivodigital.service.UtilizadorService;
import com.arquivodigital.signaling.LiveRegistry;
import com.arquivodigital.signaling.LiveSession;
import com.arquivodigital.signaling.SignalingHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/lives")
@RequiredArgsConstructor
@Tag(name = "Lives", description = "Transmissões ao vivo (WebRTC) activas")
public class LiveController {

    private final LiveRegistry registry;
    private final SignalingHandler signalingHandler;
    private final UtilizadorService utilizadorService;
    private final LogService logService;

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

    @DeleteMapping("/{id}")
    @Operation(summary = "INTERROMPER uma transmissão ao vivo (apenas ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> interromper(
            @PathVariable String id,
            @RequestParam(required = false) String motivo,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        LiveSession live = registry.porId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Live não encontrada ou já terminada: " + id));
        String titulo = live.getTitulo();
        String emissor = live.getBroadcasterNome();

        signalingHandler.encerrarPorAdmin(id, motivo);

        Utilizador admin = utilizadorService.buscarEntidade(principal.getId());
        logService.registar(AcaoLog.LIVE_INTERROMPIDA,
                "Admin interrompeu a live \"" + titulo + "\" de " + emissor
                        + (motivo != null && !motivo.isBlank() ? " — motivo: " + motivo : ""),
                admin, httpRequest.getRemoteAddr());

        return ResponseEntity.noContent().build();
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
