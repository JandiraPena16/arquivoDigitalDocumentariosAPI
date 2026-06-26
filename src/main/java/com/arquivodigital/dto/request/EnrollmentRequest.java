package com.arquivodigital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Pedido de enrolamento de um dispositivo (gera um certificado próprio)")
public class EnrollmentRequest {

    /** Identificador estável do dispositivo (ex.: ANDROID_ID ou um UUID persistido na app). */
    @NotBlank
    @Schema(example = "and-9f3a1c2b7e")
    private String deviceId;

    /** Nome amigável do dispositivo (modelo, etc.) — opcional. */
    @Schema(example = "Samsung A52 - João")
    private String deviceName;
}