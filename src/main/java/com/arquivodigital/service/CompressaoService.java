package com.arquivodigital.service;

import com.arquivodigital.dto.response.CompressaoRelatorioResponse;
import com.arquivodigital.entity.AcaoLog;
import com.arquivodigital.entity.Documentario;
import com.arquivodigital.entity.StatusDocumentario;
import com.arquivodigital.exception.custom.ResourceNotFoundException;
import com.arquivodigital.repository.DocumentarioRepository;
import com.arquivodigital.util.FFmpegUtil;
import com.arquivodigital.util.FileStorageUtil;
import com.arquivodigital.util.WhisperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompressaoService {

    private final FFmpegUtil ffmpegUtil;
    private final WhisperUtil whisperUtil;
    private final FileStorageUtil fileStorageUtil;
    private final DocumentarioRepository documentarioRepository;
    private final LogService logService;

    @Async("compressaoExecutor")
    @Transactional
    public void comprimirAsync(Long documentarioId) {
        Documentario doc = documentarioRepository.findById(documentarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Documentário não encontrado"));

        doc.setStatus(StatusDocumentario.PROCESSANDO);
        documentarioRepository.save(doc);

        try {
            String caminhoComprimido = fileStorageUtil.gerarCaminhoComprimido(doc.getCaminhoOriginal());
            String caminhoThumbnail = fileStorageUtil.gerarCaminhoThumbnail(doc.getCaminhoOriginal());

            long tamanhoOriginal = fileStorageUtil.tamanhoFicheiro(doc.getCaminhoOriginal());

            // Compressão com H.264
            long tempoMs = ffmpegUtil.comprimirVideo(doc.getCaminhoOriginal(), caminhoComprimido);

            // Thumbnail
            try {
                ffmpegUtil.extrairThumbnail(doc.getCaminhoOriginal(), caminhoThumbnail);
                doc.setCaminhoThumbnail(caminhoThumbnail);
            } catch (Exception e) {
                log.warn("Nao foi possivel extrair thumbnail para doc {}: {}", documentarioId, e.getMessage());
            }

            // Duração
            Long duracao = ffmpegUtil.obterDuracao(doc.getCaminhoOriginal());

            long tamanhoComprimido = fileStorageUtil.tamanhoFicheiro(caminhoComprimido);
            double taxa = tamanhoOriginal > 0
                    ? (1.0 - (double) tamanhoComprimido / tamanhoOriginal) * 100
                    : 0.0;

            doc.setCaminhoComprimido(caminhoComprimido);
            doc.setTamanhoOriginalBytes(tamanhoOriginal);
            doc.setTamanhoComprimidoBytes(tamanhoComprimido);
            doc.setTaxaCompressao(taxa);
            doc.setTempoProcessamentoMs(tempoMs);
            doc.setCodecVideo("H.264 (libx264)");
            doc.setCodecAudio("AAC");
            doc.setFormato("MP4");
            if (duracao != null) doc.setDuracaoSegundos(duracao);
            doc.setStatus(StatusDocumentario.PRONTO);

            // Legendas automáticas via Whisper (não bloqueia se Whisper não estiver disponível)
            try {
                String caminhoLegendas = fileStorageUtil.gerarCaminhoLegendas(doc.getCaminhoOriginal());
                whisperUtil.gerarLegendas(doc.getCaminhoOriginal(), caminhoLegendas);
                doc.setCaminhoLegendas(caminhoLegendas);
                log.info("Legendas geradas com sucesso para doc {}", documentarioId);
            } catch (Exception e) {
                log.warn("Nao foi possivel gerar legendas para doc {}: {}", documentarioId, e.getMessage());
            }

            documentarioRepository.save(doc);
            log.info("Compressao concluida para doc {}: {}% reducao", documentarioId, String.format("%.1f", taxa));

            if (doc.getUtilizador() != null) {
                logService.registar(AcaoLog.COMPRESSAO,
                        String.format("Compressao H.264 concluida: %.1f%% reducao, %dms", taxa, tempoMs),
                        doc.getUtilizador(), null);
            }

        } catch (Exception e) {
            log.error("Erro na compressao do doc {}: {}", documentarioId, e.getMessage(), e);
            doc.setStatus(StatusDocumentario.ERRO);
            documentarioRepository.save(doc);
        }
    }

    public CompressaoRelatorioResponse gerarRelatorio(Documentario doc) {
        String qualidade = avaliarQualidade(doc.getTaxaCompressao());

        return CompressaoRelatorioResponse.builder()
                .documentarioId(doc.getId())
                .titulo(doc.getTitulo())
                .tamanhoOriginalBytes(doc.getTamanhoOriginalBytes())
                .tamanhoComprimidoBytes(doc.getTamanhoComprimidoBytes())
                .tamanhoOriginalLegivel(doc.getTamanhoOriginalBytes() != null
                        ? FileStorageUtil.formatarBytes(doc.getTamanhoOriginalBytes()) : "N/A")
                .tamanhoComprimidoLegivel(doc.getTamanhoComprimidoBytes() != null
                        ? FileStorageUtil.formatarBytes(doc.getTamanhoComprimidoBytes()) : "N/A")
                .taxaCompressao(doc.getTaxaCompressao() != null
                        ? String.format("%.1f%%", doc.getTaxaCompressao()) : "N/A")
                .espacoPoupadoBytes(calcularEspacoPoupado(doc))
                .espacoPoupadoLegivel(calcularEspacoPoupadoLegivel(doc))
                .tempoProcessamentoMs(doc.getTempoProcessamentoMs())
                .tempoProcessamentoLegivel(doc.getTempoProcessamentoMs() != null
                        ? FileStorageUtil.formatarMs(doc.getTempoProcessamentoMs()) : "N/A")
                .codecVideo(doc.getCodecVideo())
                .codecAudio(doc.getCodecAudio())
                .formato(doc.getFormato())
                .qualidadePercebida(qualidade)
                .build();
    }

    private String avaliarQualidade(Double taxaCompressao) {
        if (taxaCompressao == null) return "Sem dados";
        if (taxaCompressao < 30) return "Alta (pouca compressão, máxima qualidade)";
        if (taxaCompressao < 60) return "Boa (equilíbrio qualidade/tamanho)";
        if (taxaCompressao < 80) return "Aceitável (compressão elevada, qualidade reduzida)";
        return "Baixa (compressão máxima)";
    }

    private Long calcularEspacoPoupado(Documentario doc) {
        if (doc.getTamanhoOriginalBytes() == null || doc.getTamanhoComprimidoBytes() == null) return null;
        return doc.getTamanhoOriginalBytes() - doc.getTamanhoComprimidoBytes();
    }

    private String calcularEspacoPoupadoLegivel(Documentario doc) {
        Long espacoPoupado = calcularEspacoPoupado(doc);
        return espacoPoupado != null ? FileStorageUtil.formatarBytes(espacoPoupado) : "N/A";
    }
}
