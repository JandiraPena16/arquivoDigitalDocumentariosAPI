package com.arquivodigital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para actualizar o perfil do utilizador autenticado")
public class UpdatePerfilRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100)
    @Schema(example = "João Silva Actualizado")
    private String nome;
}
