package com.arquivodigital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Credenciais de login")
public class LoginRequest {

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Schema(example = "admin@arquivo.ao")
    private String email;

    @NotBlank(message = "Password é obrigatória")
    @Schema(example = "Admin@2024")
    private String password;
}
