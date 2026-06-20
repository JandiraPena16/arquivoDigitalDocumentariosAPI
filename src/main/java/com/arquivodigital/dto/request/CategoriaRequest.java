package com.arquivodigital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados de uma categoria")
public class CategoriaRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100)
    @Schema(example = "História")
    private String nome;

    @Schema(example = "Documentários históricos e arquivos culturais")
    private String descricao;
}
