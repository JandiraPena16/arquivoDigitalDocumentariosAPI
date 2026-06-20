package com.arquivodigital.util;

import com.arquivodigital.exception.custom.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Component
@Slf4j
public class FileStorageUtil {

    @Value("${arquivo.storage.originais}")
    private String dirOriginais;

    @Value("${arquivo.storage.comprimidos}")
    private String dirComprimidos;

    @Value("${arquivo.storage.thumbnails}")
    private String dirThumbnails;

    @Value("${arquivo.storage.temp}")
    private String dirTemp;

    public void inicializar() {
        criarDiretorio(dirOriginais);
        criarDiretorio(dirComprimidos);
        criarDiretorio(dirThumbnails);
        criarDiretorio(dirTemp);
        log.info("Diretorios de armazenamento iniciados");
    }

    public String guardarOriginal(MultipartFile file) {
        String extensao = obterExtensao(file.getOriginalFilename());
        String nomeUnico = UUID.randomUUID() + "." + extensao;
        Path destino = Paths.get(dirOriginais, nomeUnico);
        guardarFicheiro(file, destino);
        return destino.toAbsolutePath().toString();
    }

    public String gerarCaminhoComprimido(String caminhoOriginal) {
        Path original = Paths.get(caminhoOriginal);
        String nome = original.getFileName().toString();
        String semExtensao = nome.contains(".") ? nome.substring(0, nome.lastIndexOf('.')) : nome;
        return Paths.get(dirComprimidos, semExtensao + "_compressed.mp4").toAbsolutePath().toString();
    }

    public String gerarCaminhoThumbnail(String caminhoOriginal) {
        Path original = Paths.get(caminhoOriginal);
        String nome = original.getFileName().toString();
        String semExtensao = nome.contains(".") ? nome.substring(0, nome.lastIndexOf('.')) : nome;
        return Paths.get(dirThumbnails, semExtensao + "_thumb.jpg").toAbsolutePath().toString();
    }

    public void eliminar(String caminho) {
        if (caminho == null) return;
        try {
            Files.deleteIfExists(Paths.get(caminho));
        } catch (IOException e) {
            log.warn("Nao foi possivel eliminar ficheiro: {}", caminho);
        }
    }

    public boolean existe(String caminho) {
        return caminho != null && Files.exists(Paths.get(caminho));
    }

    public long tamanhoFicheiro(String caminho) {
        try {
            return Files.size(Paths.get(caminho));
        } catch (IOException e) {
            return 0L;
        }
    }

    public Path obterPath(String caminho) {
        return Paths.get(caminho);
    }

    private void guardarFicheiro(MultipartFile file, Path destino) {
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Erro ao guardar ficheiro: " + file.getOriginalFilename(), e);
        }
    }

    private void criarDiretorio(String caminho) {
        try {
            Files.createDirectories(Paths.get(caminho));
        } catch (IOException e) {
            throw new FileStorageException("Nao foi possivel criar diretório: " + caminho, e);
        }
    }

    public static String obterExtensao(String nomeOriginal) {
        if (nomeOriginal == null || !nomeOriginal.contains(".")) return "bin";
        return nomeOriginal.substring(nomeOriginal.lastIndexOf('.') + 1).toLowerCase();
    }

    public static String formatarBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static String formatarMs(long ms) {
        if (ms < 1000) return ms + "ms";
        long segundos = ms / 1000;
        if (segundos < 60) return segundos + "s";
        return String.format("%dm %ds", segundos / 60, segundos % 60);
    }
}
