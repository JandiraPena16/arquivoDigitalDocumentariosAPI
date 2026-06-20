package com.arquivodigital.controller;

import com.arquivodigital.dto.response.LogResponse;
import com.arquivodigital.dto.response.PageResponse;
import com.arquivodigital.entity.AcaoLog;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.security.UserDetailsImpl;
import com.arquivodigital.service.LogService;
import com.arquivodigital.service.UtilizadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "Logs", description = "Registo de actividade do sistema")
@SecurityRequirement(name = "bearerAuth")
public class LogController {

    private final LogService logService;
    private final UtilizadorService utilizadorService;

    @GetMapping
    @Operation(summary = "Listar todos os logs (apenas ADMIN) — com filtros opcionais")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<LogResponse>> listarTodos(
            @RequestParam(required = false) AcaoLog acao,
            @RequestParam(required = false) Long utilizadorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho
    ) {
        return ResponseEntity.ok(logService.listarTodos(acao, utilizadorId, pagina, tamanho));
    }

    @GetMapping("/meus")
    @Operation(summary = "Listar os meus logs de actividade")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<LogResponse>> meus(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        return ResponseEntity.ok(logService.listarPorUtilizador(utilizador, pagina, tamanho));
    }

    @GetMapping("/utilizador/{id}")
    @Operation(summary = "Listar logs de um utilizador específico (apenas ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<LogResponse>> porUtilizador(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(id);
        return ResponseEntity.ok(logService.listarPorUtilizador(utilizador, pagina, tamanho));
    }

    @DeleteMapping
    @Operation(summary = "Limpar todos os logs (apenas ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> limpar() {
        logService.limparTodos();
        return ResponseEntity.ok(Map.of("mensagem", "Todos os logs foram eliminados"));
    }
}
