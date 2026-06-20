package com.arquivodigital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Resposta paginada")
public class PageResponse<T> {
    private List<T> conteudo;
    private int paginaActual;
    private int totalPaginas;
    private long totalElementos;
    private int tamanhoPagina;
    private boolean primeira;
    private boolean ultima;
}
