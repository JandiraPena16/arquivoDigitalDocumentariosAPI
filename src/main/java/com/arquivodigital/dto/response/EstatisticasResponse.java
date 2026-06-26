package com.arquivodigital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EstatisticasResponse {
    private long totalDocumentarios;
    private String totalOriginalLegivel;
    private String totalComprimidoLegivel;
    private String totalPoupadoLegivel;
    private long totalPoupadoBytes;
    private String taxaMediaCompressao;
    private Map<String, Long> porEstado;
    private Map<String, Long> porCategoria;
}
