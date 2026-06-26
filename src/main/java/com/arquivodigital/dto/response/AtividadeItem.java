package com.arquivodigital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @AllArgsConstructor @NoArgsConstructor
public class AtividadeItem {
    private Long documentarioId;
    private String titulo;
    private Integer valor;        // 1=like, -1=dislike, null para lista/histórico
    private LocalDateTime data;
}
