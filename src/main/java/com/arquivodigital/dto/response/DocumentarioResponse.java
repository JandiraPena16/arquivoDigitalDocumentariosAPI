package com.arquivodigital.dto.response;

import com.arquivodigital.entity.StatusDocumentario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Dados completos de um documentário")
public class DocumentarioResponse {

    private Long id;
    private String titulo;
    private String descricao;
    private Integer ano;
    private Long duracaoSegundos;
    private String formato;
    private StatusDocumentario status;
    private CategoriaResponse categoria;
    private String nomeUtilizador;

    // URLs para acesso
    @Schema(description = "URL para streaming do documentário")
    private String urlStreaming;

    @Schema(description = "URL para download do documentário")
    private String urlDownload;

    @Schema(description = "URL do thumbnail")
    private String urlThumbnail;

    @Schema(description = "URL das legendas automáticas (WebVTT), null se não disponível")
    private String urlLegendas;

    // Compressão
    private Long tamanhoOriginalBytes;
    private Long tamanhoComprimidoBytes;
    private Double taxaCompressao;
    private Long tempoProcessamentoMs;
    private String codecVideo;
    private String codecAudio;

    // Métricas
    private Long visualizacoes;
    private Long downloads;
    private Long likeCount;

    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
}
