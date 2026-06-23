package com.arquivodigital.service;

import com.arquivodigital.entity.AcaoLog;
import com.arquivodigital.entity.Documentario;
import com.arquivodigital.entity.StatusDocumentario;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.exception.custom.NegocioException;
import com.arquivodigital.exception.custom.ResourceNotFoundException;
import com.arquivodigital.repository.DocumentarioRepository;
import com.arquivodigital.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingService {

    private final DocumentarioRepository documentarioRepository;
    private final FileStorageUtil fileStorageUtil;
    private final LogService logService;

    private static final int CHUNK_SIZE = 1024 * 1024; // 1 MB por chunk

    @Transactional
    public ResponseEntity<Resource> stream(Long id, String rangeHeader, Utilizador utilizador, String ip) {
        Documentario doc = buscarPronto(id);

        // Usar ficheiro comprimido se disponível, senão usar original
        String caminho = fileStorageUtil.existe(doc.getCaminhoComprimido())
                ? doc.getCaminhoComprimido()
                : doc.getCaminhoOriginal();

        if (!fileStorageUtil.existe(caminho)) {
            throw new ResourceNotFoundException("Ficheiro de vídeo não encontrado no servidor");
        }

        Path videoPath = fileStorageUtil.obterPath(caminho);
        long fileSize;
        try {
            fileSize = Files.size(videoPath);
        } catch (IOException e) {
            throw new NegocioException("Erro ao aceder ao ficheiro de vídeo");
        }

        documentarioRepository.incrementarVisualizacoes(id);
        if (utilizador != null) {
            logService.registar(AcaoLog.STREAMING, "Streaming: " + doc.getTitulo(), utilizador, ip);
        }

        String contentType = determinarContentType(caminho);

        // Sem Range header — devolve ficheiro completo
        if (rangeHeader == null) {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, contentType);
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getTitulo() + ".mp4\"");
            return ResponseEntity.ok().headers(headers).body(new FileSystemResource(videoPath));
        }

        // Com Range header — streaming parcial (HTTP 206)
        return processarRange(videoPath, rangeHeader, fileSize, contentType, doc.getTitulo());
    }

    public ResponseEntity<Resource> thumbnail(Long id) {
        Documentario doc = documentarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documentário não encontrado: " + id));

        if (!fileStorageUtil.existe(doc.getCaminhoThumbnail())) {
            throw new ResourceNotFoundException("Thumbnail não disponível para este documentário");
        }

        String caminho = doc.getCaminhoThumbnail().toLowerCase();
        String tipo = caminho.endsWith(".webp") ? "image/webp"
                : caminho.endsWith(".png") ? MediaType.IMAGE_PNG_VALUE
                : MediaType.IMAGE_JPEG_VALUE;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, tipo);
        return ResponseEntity.ok().headers(headers)
                .body(new FileSystemResource(fileStorageUtil.obterPath(doc.getCaminhoThumbnail())));
    }

    private ResponseEntity<Resource> processarRange(Path videoPath, String rangeHeader, long fileSize, String contentType, String titulo) {
        try {
            long start, end;

            String rangeValue = rangeHeader.substring("bytes=".length());
            String[] parts = rangeValue.split("-");

            start = Long.parseLong(parts[0]);
            end = (parts.length > 1 && !parts[1].isEmpty())
                    ? Long.parseLong(parts[1])
                    : Math.min(start + CHUNK_SIZE - 1, fileSize - 1);

            if (start >= fileSize || end >= fileSize) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header("Content-Range", "bytes */" + fileSize)
                        .build();
            }

            long contentLength = end - start + 1;

            try (RandomAccessFile raf = new RandomAccessFile(videoPath.toFile(), "r")) {
                raf.seek(start);
                byte[] data = new byte[(int) contentLength];
                raf.readFully(data);

                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.CONTENT_TYPE, contentType);
                headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
                headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
                headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + titulo + ".mp4\"");

                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .headers(headers)
                        .body(new org.springframework.core.io.ByteArrayResource(data));
            }

        } catch (Exception e) {
            log.error("Erro ao processar Range request: {}", e.getMessage());
            throw new NegocioException("Erro ao processar pedido de streaming: " + e.getMessage());
        }
    }

    private Documentario buscarPronto(Long id) {
        Documentario doc = documentarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documentário não encontrado: " + id));

        if (doc.getStatus() == StatusDocumentario.PROCESSANDO) {
            throw new NegocioException("O documentário ainda está a ser processado. Tente novamente em breve.");
        }
        if (doc.getStatus() == StatusDocumentario.PENDENTE) {
            throw new NegocioException("O documentário está pendente de processamento.");
        }
        if (doc.getStatus() == StatusDocumentario.ERRO) {
            // Permite streaming do original mesmo com erro na compressão
            log.warn("Streaming do documentário {} com erro na compressao — usando ficheiro original", id);
        }
        return doc;
    }

    private String determinarContentType(String caminho) {
        String ext = FileStorageUtil.obterExtensao(caminho);
        return switch (ext) {
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mkv" -> "video/x-matroska";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            default -> "application/octet-stream";
        };
    }
}
