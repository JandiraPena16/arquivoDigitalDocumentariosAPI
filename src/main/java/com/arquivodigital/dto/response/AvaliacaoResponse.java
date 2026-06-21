package com.arquivodigital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AvaliacaoResponse {
    private Long documentarioId;
    /** 1 = gosto, -1 = não gosto, null = sem avaliação */
    private Integer valor;
}
