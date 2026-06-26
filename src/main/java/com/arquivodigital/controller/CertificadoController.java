package com.arquivodigital.controller;

import com.arquivodigital.dto.request.RegistarCertificadoRequest;
import com.arquivodigital.dto.response.CertificadoResponse;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.security.UserDetailsImpl;
import com.arquivodigital.service.CertificadoService;
import com.arquivodigital.service.UtilizadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/certificados")
@RequiredArgsConstructor
@Tag(name = "Certificados", description = "Registo e gestão dos certificados de dispositivo (mTLS/PKI)")
@SecurityRequirement(name = "bearerAuth")
public class CertificadoController {

    private final CertificadoService certificadoService;
    private final UtilizadorService utilizadorService;

    @Value("${arquivo.pki.ca-cert:./pki/ca/ca.crt}")
    private String caCertPath;

    @GetMapping
    @Operation(summary = "Listar certificados de dispositivo (apenas ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CertificadoResponse>> listar() {
        return ResponseEntity.ok(certificadoService.listar());
    }

    @PostMapping
    @Operation(summary = "Registar metadados de um certificado emitido (apenas ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CertificadoResponse> registar(
            @Valid @RequestBody RegistarCertificadoRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        Utilizador admin = utilizadorService.buscarEntidade(principal.getId());
        return ResponseEntity.ok(certificadoService.registar(request, admin, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/{id}/revogar")
    @Operation(summary = "Revogar um certificado de dispositivo (apenas ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revogar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        Utilizador admin = utilizadorService.buscarEntidade(principal.getId());
        certificadoService.revogar(id, admin, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ca")
    @Operation(summary = "Descarregar o certificado da CA (.crt) para instalar nos dispositivos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> descarregarCa() {
        File f = new File(caCertPath);
        if (!f.exists()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(f);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"arquivo-digital-ca.crt\"")
                .contentType(MediaType.parseMediaType("application/x-x509-ca-cert"))
                .body(resource);
    }
}