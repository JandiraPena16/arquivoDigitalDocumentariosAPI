package com.arquivodigital.controller;

import com.arquivodigital.dto.request.AdminUpdateUserRequest;
import com.arquivodigital.dto.request.ChangePasswordRequest;
import com.arquivodigital.dto.request.UpdatePerfilRequest;
import com.arquivodigital.dto.response.PageResponse;
import com.arquivodigital.dto.response.UtilizadorResponse;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.security.UserDetailsImpl;
import com.arquivodigital.service.UtilizadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/utilizadores")
@RequiredArgsConstructor
@Tag(name = "Utilizadores", description = "Gestão de perfil e administração de utilizadores")
@SecurityRequirement(name = "bearerAuth")
public class UtilizadorController {

    private final UtilizadorService utilizadorService;

    @GetMapping
    @Operation(summary = "Listar todos os utilizadores (apenas ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UtilizadorResponse>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho
    ) {
        return ResponseEntity.ok(utilizadorService.listarTodos(pagina, tamanho));
    }

    @GetMapping("/perfil")
    @Operation(summary = "Obter perfil do utilizador autenticado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UtilizadorResponse> perfil(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(utilizadorService.buscarPorId(principal.getId()));
    }

    @PutMapping("/perfil")
    @Operation(summary = "Actualizar perfil do utilizador autenticado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UtilizadorResponse> actualizarPerfil(
            @Valid @RequestBody UpdatePerfilRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        return ResponseEntity.ok(utilizadorService.actualizarPerfil(utilizador, request));
    }

    @PutMapping("/perfil/password")
    @Operation(summary = "Alterar password (revoga todas as sessões por segurança)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> alterarPassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        utilizadorService.alterarPassword(utilizador, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("mensagem", "Password alterada com sucesso. Faça login novamente."));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar utilizador por ID (apenas ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilizadorResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(utilizadorService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar utilizador (apenas ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilizadorResponse> adminActualizar(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        Utilizador admin = utilizadorService.buscarEntidade(principal.getId());
        return ResponseEntity.ok(utilizadorService.adminActualizar(id, request, admin, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar utilizador (apenas ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        Utilizador admin = utilizadorService.buscarEntidade(principal.getId());
        utilizadorService.eliminar(id, admin, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
