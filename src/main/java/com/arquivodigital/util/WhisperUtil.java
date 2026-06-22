package com.arquivodigital.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class WhisperUtil {

    @Value("${arquivo.whisper.path:python -m whisper}")
    private String whisperPath;

    @Value("${arquivo.whisper.model:base}")
    private String model;

    @Value("${arquivo.whisper.language:pt}")
    private String language;

    @Value("${arquivo.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${arquivo.storage.temp:./uploads/temp}")
    private String dirTemp;

    /**
     * Gera legendas WebVTT para o vídeo dado.
     * Extrai o áudio com FFmpeg e depois transcreve com Whisper CLI.
     * Suporta tanto "whisper" (Linux/Mac) como "python -m whisper" (Windows).
     */
    public String gerarLegendas(String caminhoVideo, String caminhoVttDestino) throws Exception {
        String nomeTemp = UUID.randomUUID().toString();
        Path dirTempPath = Paths.get(dirTemp);
        Files.createDirectories(dirTempPath);
        String audioTemp = dirTempPath.resolve(nomeTemp + ".wav").toAbsolutePath().toString();

        // 1. Extrair áudio em WAV 16kHz mono (formato ideal para Whisper)
        List<String> cmdAudio = List.of(
                ffmpegPath,
                "-i", caminhoVideo,
                "-vn",
                "-acodec", "pcm_s16le",
                "-ar", "16000",
                "-ac", "1",
                "-y", audioTemp
        );
        executar(cmdAudio, "Extração de áudio para legendas");

        // 2. Correr Whisper para gerar o VTT
        Path outputDir = Paths.get(caminhoVttDestino).getParent();
        Files.createDirectories(outputDir);

        // Divide "python -m whisper" em ["python", "-m", "whisper"] para o ProcessBuilder
        List<String> cmdWhisper = new ArrayList<>(Arrays.asList(whisperPath.split("\\s+")));
        cmdWhisper.add(audioTemp);
        cmdWhisper.add("--model");
        cmdWhisper.add(model);
        cmdWhisper.add("--output_format");
        cmdWhisper.add("vtt");
        cmdWhisper.add("--output_dir");
        cmdWhisper.add(outputDir.toAbsolutePath().toString());
        if (language != null && !language.isBlank()) {
            cmdWhisper.add("--language");
            cmdWhisper.add(language);
        }
        executar(cmdWhisper, "Geração de legendas (Whisper)");

        // Whisper nomeia o ficheiro de saída igual ao ficheiro de entrada (sem extensão) + .vtt
        Path whisperSaida = outputDir.resolve(nomeTemp + ".vtt");
        if (!Files.exists(whisperSaida)) {
            throw new RuntimeException("Ficheiro VTT não foi gerado pelo Whisper: " + whisperSaida);
        }
        Files.move(whisperSaida, Paths.get(caminhoVttDestino), StandardCopyOption.REPLACE_EXISTING);

        // Limpar ficheiro de áudio temporário
        try { Files.deleteIfExists(Paths.get(audioTemp)); } catch (Exception ignored) {}

        log.info("Legendas geradas com sucesso: {}", caminhoVttDestino);
        return caminhoVttDestino;
    }

    public boolean whisperDisponivel() {
        try {
            List<String> cmd = new ArrayList<>(Arrays.asList(whisperPath.split("\\s+")));
            cmd.add("--help");
            int exitCode = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.warn("Whisper CLI não encontrado em '{}'. Geração de legendas desactivada.", whisperPath);
            return false;
        }
    }

    private void executar(List<String> cmd, String operacao) throws Exception {
        log.info("Iniciando {}: {}", operacao, String.join(" ", cmd));
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
        if (exitCode != 0) {
            log.error("{} output: {}", operacao, output);
            throw new RuntimeException(operacao + " falhou (exit=" + exitCode + "): "
                    + output.toString().lines().reduce("", (a, b) -> b));
        }
        log.info("{} concluída com sucesso", operacao);
    }
}
