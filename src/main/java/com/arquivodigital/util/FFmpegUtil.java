package com.arquivodigital.util;

import com.arquivodigital.exception.custom.CompressaoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class FFmpegUtil {

    @Value("${arquivo.ffmpeg.path}")
    private String ffmpegPath;

    @Value("${arquivo.ffmpeg.ffprobe-path}")
    private String ffprobePath;

    @Value("${arquivo.compressao.video-codec}")
    private String videoCodec;

    @Value("${arquivo.compressao.audio-codec}")
    private String audioCodec;

    @Value("${arquivo.compressao.crf}")
    private String crf;

    @Value("${arquivo.compressao.preset}")
    private String preset;

    @Value("${arquivo.compressao.webp-quality:90}")
    private String webpQuality;

    /**
     * Comprime vídeo com H.264 (libx264) usando FFmpeg.
     * Retorna o tempo de processamento em ms.
     */
    public long comprimirVideo(String entrada, String saida) {
        List<String> cmd = List.of(
                ffmpegPath,
                "-i", entrada,
                "-c:v", videoCodec,
                "-crf", crf,
                "-preset", preset,
                "-c:a", audioCodec,
                "-b:a", "128k",
                "-movflags", "+faststart",
                "-y",
                saida
        );
        return executar(cmd, "Compressão H.264");
    }

    /**
     * Comprime vídeo com H.265 (libx265).
     */
    public long comprimirVideoH265(String entrada, String saida) {
        List<String> cmd = List.of(
                ffmpegPath,
                "-i", entrada,
                "-c:v", "libx265",
                "-crf", "28",
                "-preset", preset,
                "-c:a", "aac",
                "-b:a", "128k",
                "-tag:v", "hvc1",
                "-y",
                saida
        );
        return executar(cmd, "Compressão H.265");
    }

    /**
     * Comprime uma imagem (capa) para WebP de alta qualidade.
     * Usa quality=90 (visualmente sem perda) e o codificador libwebp do FFmpeg,
     * reduzindo significativamente o tamanho face a JPEG/PNG sem degradação visível.
     * Retorna o tempo de processamento em ms.
     */
    public long comprimirImagemWebp(String entrada, String saida) {
        List<String> cmd = List.of(
                ffmpegPath,
                "-i", entrada,
                "-c:v", "libwebp",
                "-quality", webpQuality,
                "-preset", "picture",
                "-y",
                saida
        );
        return executar(cmd, "Compressão de imagem (WebP)");
    }

    /**
     * Extrai thumbnail do segundo 10 do vídeo.
     */
    public void extrairThumbnail(String entrada, String saida) {
        List<String> cmd = List.of(
                ffmpegPath,
                "-i", entrada,
                "-ss", "00:00:10",
                "-frames:v", "1",
                "-vf", "scale=320:-1",
                "-y",
                saida
        );
        executar(cmd, "Extração de thumbnail");
    }

    /**
     * Obtém a duração do vídeo em segundos usando ffprobe.
     */
    public Long obterDuracao(String caminho) {
        try {
            List<String> cmd = List.of(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    caminho
            );
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                process.waitFor();
                if (line != null) {
                    return (long) Double.parseDouble(line.trim());
                }
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel obter duracao do video: {}", e.getMessage());
        }
        return null;
    }

    public boolean ffmpegDisponivel() {
        try {
            new ProcessBuilder(ffmpegPath, "-version").start().waitFor();
            return true;
        } catch (Exception e) {
            log.warn("FFmpeg nao encontrado no PATH. Compressao desactivada.");
            return false;
        }
    }

    private long executar(List<String> cmd, String operacao) {
        log.info("Iniciando {}: {}", operacao, String.join(" ", cmd));
        long inicio = System.currentTimeMillis();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            long duracao = System.currentTimeMillis() - inicio;

            if (exitCode != 0) {
                log.error("FFmpeg falhou (exit {}): {}", exitCode, output.toString().lines().reduce("", (a, b) -> b));
                throw new CompressaoException("FFmpeg terminou com erro (exit code " + exitCode + ")");
            }

            log.info("{} concluida em {}ms", operacao, duracao);
            return duracao;

        } catch (CompressaoException e) {
            throw e;
        } catch (Exception e) {
            throw new CompressaoException("Erro ao executar " + operacao + ": " + e.getMessage(), e);
        }
    }
}
