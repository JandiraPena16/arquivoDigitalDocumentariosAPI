package com.arquivodigital.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvaliacaoRequest {

    @NotNull(message = "O valor é obrigatório")
    @Min(value = -1, message = "Valor mínimo é -1 (não gosto)")
    @Max(value = 1, message = "Valor máximo é 1 (gosto)")
    private Integer valor;
}
