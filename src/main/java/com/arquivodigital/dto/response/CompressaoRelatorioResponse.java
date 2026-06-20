package com.arquivodigital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Relatório comparativo de compressão multimédia")
public class CompressaoRelatorioResponse {

    private Long documentarioId;
    private String titulo;

    @Schema(description = "Tamanho do ficheiro original em bytes")
    private Long tamanhoOriginalBytes;

    @Schema(description = "Tamanho do ficheiro comprimido em bytes")
    private Long tamanhoComprimidoBytes;

    @Schema(description = "Tamanho original legível (ex: 250 MB)")
    private String tamanhoOriginalLegivel;

    @Schema(description = "Tamanho comprimido legível (ex: 85 MB)")
    private String tamanhoComprimidoLegivel;

    @Schema(description = "Taxa de compressão em percentagem (ex: 66.0%)")
    private String taxaCompressao;

    @Schema(description = "Espaço poupado em bytes")
    private Long espacoPoupadoBytes;

    @Schema(description = "Espaço poupado legível")
    private String espacoPoupadoLegivel;

    @Schema(description = "Tempo de processamento em milissegundos")
    private Long tempoProcessamentoMs;

    @Schema(description = "Tempo de processamento legível (ex: 2m 30s)")
    private String tempoProcessamentoLegivel;

    @Schema(description = "Codec de vídeo utilizado (ex: H.264, H.265)")
    private String codecVideo;

    @Schema(description = "Codec de áudio utilizado (ex: AAC, MP3)")
    private String codecAudio;

    @Schema(description = "Formato do ficheiro comprimido")
    private String formato;

    @Schema(description = "Qualidade percebida (estimativa baseada no CRF)")
    private String qualidadePercebida;
}
