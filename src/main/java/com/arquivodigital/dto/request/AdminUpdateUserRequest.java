package com.arquivodigital.dto.request;

import com.arquivodigital.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para actualização de utilizador pelo administrador")
public class AdminUpdateUserRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100)
    @Schema(example = "Nome Actualizado")
    private String nome;

    @Schema(example = "USER")
    private Role role;

    @Schema(example = "true")
    private Boolean ativo;
}
