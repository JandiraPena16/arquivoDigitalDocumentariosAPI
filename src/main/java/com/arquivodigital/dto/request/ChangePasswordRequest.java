package com.arquivodigital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para alteração de password")
public class ChangePasswordRequest {

    @NotBlank(message = "Password actual é obrigatória")
    @Schema(example = "SenhaAntiga@123")
    private String passwordActual;

    @NotBlank(message = "Nova password é obrigatória")
    @Size(min = 6, max = 100)
    @Schema(example = "NovaSenha@123")
    private String novaPassword;
}
