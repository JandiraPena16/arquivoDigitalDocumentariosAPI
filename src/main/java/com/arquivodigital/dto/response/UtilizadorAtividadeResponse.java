package com.arquivodigital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class UtilizadorAtividadeResponse {
    private List<AtividadeItem> avaliacoes;  // likes/dislikes
    private List<AtividadeItem> lista;        // "ver mais tarde"
    private List<AtividadeItem> historico;    // já assistidos
}
