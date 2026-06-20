package com.arquivodigital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Metadados do documentário (enviados como form-data junto com o ficheiro)")
public class DocumentarioRequest {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200)
    @Schema(example = "Angola: Memória e Identidade")
    private String titulo;

    @Schema(example = "Documentário sobre a história e cultura angolana")
    private String descricao;

    @Schema(example = "2024")
    private Integer ano;

    @Schema(example = "1")
    private Long categoriaId;
}
