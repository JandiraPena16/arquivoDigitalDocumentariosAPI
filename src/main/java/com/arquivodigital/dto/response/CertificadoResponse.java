package com.arquivodigital.dto.response;

import com.arquivodigital.entity.EstadoCertificado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CertificadoResponse {
    private Long id;
    private String deviceId;
    private String dono;
    private String serial;
    private String fingerprint;
    private LocalDateTime emitidoEm;
    private LocalDateTime validadeAte;
    private EstadoCertificado estado;
    private LocalDateTime revogadoEm;
    private LocalDateTime ultimaUtilizacao;
}