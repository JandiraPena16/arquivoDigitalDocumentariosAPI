package com.arquivodigital.controller;

import com.arquivodigital.dto.request.EnrollmentRequest;
import com.arquivodigital.dto.response.EnrollmentResponse;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.security.UserDetailsImpl;
import com.arquivodigital.service.EnrollmentService;
import com.arquivodigital.service.UtilizadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollment")
@RequiredArgsConstructor
@Tag(name = "Enrolamento", description = "Emissão automática de certificado por dispositivo (mTLS)")
@SecurityRequirement(name = "bearerAuth")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final UtilizadorService utilizadorService;

    @PostMapping
    @Operation(summary = "Enrolar o dispositivo: gera e devolve um certificado próprio (requer login)")
    @PreAuthorize("isAuthenticated()")
    public EnrollmentResponse enrolar(
            @Valid @RequestBody EnrollmentRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        Utilizador dono = utilizadorService.buscarEntidade(principal.getId());
        return enrollmentService.enrolar(request.getDeviceId(), request.getDeviceName(),
                dono, httpRequest.getRemoteAddr());
    }
}