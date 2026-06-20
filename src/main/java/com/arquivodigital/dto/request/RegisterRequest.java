package com.arquivodigital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados de registo de novo utilizador")
public class RegisterRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100)
    @Schema(example = "João Silva")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Schema(example = "joao@email.com")
    private String email;

    @NotBlank(message = "Password é obrigatória")
    @Size(min = 6, max = 100, message = "Password deve ter no mínimo 6 caracteres")
    @Schema(example = "Senha@123")
    private String password;
}
