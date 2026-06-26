package com.arquivodigital.controller;

import com.arquivodigital.dto.response.NotificacaoResponse;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.security.UserDetailsImpl;
import com.arquivodigital.service.NotificacaoService;
import com.arquivodigital.service.UtilizadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
@Tag(name = "Notificações", description = "Notificações do utilizador")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;
    private final UtilizadorService utilizadorService;

    @GetMapping
    @Operation(summary = "Listar as minhas notificações")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificacaoResponse>> listar(@AuthenticationPrincipal UserDetailsImpl principal) {
        Utilizador u = utilizadorService.buscarEntidade(principal.getId());
        return ResponseEntity.ok(notificacaoService.listar(u));
    }

    @GetMapping("/nao-lidas")
    @Operation(summary = "Número de notificações não lidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> naoLidas(@AuthenticationPrincipal UserDetailsImpl principal) {
        Utilizador u = utilizadorService.buscarEntidade(principal.getId());
        return ResponseEntity.ok(Map.of("count", notificacaoService.contarNaoLidas(u)));
    }

    @PostMapping("/marcar-lidas")
    @Operation(summary = "Marcar todas as notificações como lidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> marcarLidas(@AuthenticationPrincipal UserDetailsImpl principal) {
        Utilizador u = utilizadorService.buscarEntidade(principal.getId());
        notificacaoService.marcarTodasLidas(u);
        return ResponseEntity.noContent().build();
    }
}
