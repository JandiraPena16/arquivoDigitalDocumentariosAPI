package com.arquivodigital.controller;

import com.arquivodigital.dto.request.LoginRequest;
import com.arquivodigital.dto.request.RegisterRequest;
import com.arquivodigital.dto.response.AuthResponse;
import com.arquivodigital.dto.response.SessaoResponse;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.security.UserDetailsImpl;
import com.arquivodigital.service.AuthService;
import com.arquivodigital.service.SessaoService;
import com.arquivodigital.service.UtilizadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Registo, login, logout e gestão de sessões")
public class AuthController {

    private final AuthService authService;
    private final SessaoService sessaoService;
    private final UtilizadorService utilizadorService;

    @PostMapping("/registar")
    @Operation(summary = "Registar novo utilizador")
    public ResponseEntity<AuthResponse> registar(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.registar(request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Fazer login e obter token JWT + ID de sessão")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.login(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Fazer logout (revoga a sessão actual)", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> logout(
            @RequestParam String sessaoId,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        authService.logout(sessaoId, utilizador, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("mensagem", "Logout efectuado com sucesso"));
    }

    @GetMapping("/sessoes")
    @Operation(summary = "Listar todas as sessões (activas e inactivas) do utilizador actual",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SessaoResponse>> listarSessoes(
            @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        return ResponseEntity.ok(sessaoService.listarSessoesDo(utilizador));
    }

    @DeleteMapping("/sessoes/{sessaoId}")
    @Operation(summary = "Revogar uma sessão específica",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> revogarSessao(
            @PathVariable String sessaoId,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        sessaoService.revogar(sessaoId, utilizador);
        return ResponseEntity.ok(Map.of("mensagem", "Sessão " + sessaoId + " revogada"));
    }

    @DeleteMapping("/sessoes")
    @Operation(summary = "Revogar todas as sessões do utilizador actual (logout em todos os dispositivos)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> revogarTodasSessoes(
            @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        sessaoService.revogarTodasDoUtilizador(utilizador);
        return ResponseEntity.ok(Map.of("mensagem", "Todas as sessões foram revogadas"));
    }
}
