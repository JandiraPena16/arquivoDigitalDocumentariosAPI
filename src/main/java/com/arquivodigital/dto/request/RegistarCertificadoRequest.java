package com.arquivodigital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Metadados de um certificado de dispositivo emitido pela CA")
public class RegistarCertificadoRequest {

    @NotBlank
    @Schema(example = "dispositivo-001")
    private String deviceId;

    @Schema(example = "joao@exemplo.com")
    private String dono;

    @NotBlank
    @Schema(example = "41567340699331897A2F74D1A1D920BE8AF27A70")
    private String serial;

    @Schema(example = "78:61:21:C2:...")
    private String fingerprint;

    @Schema(example = "2028-09-27T15:19:44")
    private LocalDateTime validadeAte;
}