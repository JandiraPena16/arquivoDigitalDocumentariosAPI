package com.arquivodigital.service;

import com.arquivodigital.dto.request.DocumentarioRequest;
import com.arquivodigital.dto.response.CompressaoRelatorioResponse;
import com.arquivodigital.dto.response.DocumentarioResponse;
import com.arquivodigital.dto.response.PageResponse;
import com.arquivodigital.entity.*;
import com.arquivodigital.exception.custom.NegocioException;
import com.arquivodigital.exception.custom.ResourceNotFoundException;
import com.arquivodigital.repository.DocumentarioRepository;
import com.arquivodigital.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentarioService {

    private final DocumentarioRepository documentarioRepository;
    private final FileStorageUtil fileStorageUtil;
    private final CategoriaService categoriaService;
    private final CompressaoService compressaoService;
    private final LogService logService;

    private static final String BASE_URL = "http://localhost:8080";

    @Transactional
    public DocumentarioResponse upload(MultipartFile ficheiro, DocumentarioRequest request, Utilizador utilizador, String ip) {
        validarFicheiroVideo(ficheiro);

        String caminhoOriginal = fileStorageUtil.guardarOriginal(ficheiro);

        Categoria categoria = null;
        if (request.getCategoriaId() != null) {
            categoria = categoriaService.buscarEntidade(request.getCategoriaId());
        }

        Documentario doc = Documentario.builder()
                .titulo(request.getTitulo())
                .descricao(request.getDescricao())
                .ano(request.getAno())
                .caminhoOriginal(caminhoOriginal)
                .tamanhoOriginalBytes(ficheiro.getSize())
                .formato(FileStorageUtil.obterExtensao(ficheiro.getOriginalFilename()))
                .status(StatusDocumentario.PENDENTE)
                .utilizador(utilizador)
                .categoria(categoria)
                .build();

        doc = documentarioRepository.save(doc);

        // Dispara compressão apenas após o commit da transação (evita "not found" no thread async)
        final Long docId = doc.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                compressaoService.comprimirAsync(docId);
            }
        });

        logService.registar(AcaoLog.UPLOAD,
                "Upload do documentário: " + request.getTitulo() + " (" + FileStorageUtil.formatarBytes(ficheiro.getSize()) + ")",
                utilizador, ip);

        return toResponse(doc);
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentarioResponse> listar(int pagina, int tamanho) {
        Page<Documentario> page = documentarioRepository.findByStatus(
                StatusDocumentario.PRONTO,
                PageRequest.of(pagina, tamanho, Sort.by("dataCriacao").descending())
        );
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentarioResponse> pesquisar(String q, Long categoriaId, Integer ano, int pagina, int tamanho) {
        Page<Documentario> page = documentarioRepository.pesquisar(
                q, categoriaId, ano,
                PageRequest.of(pagina, tamanho, Sort.by("dataCriacao").descending())
        );
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentarioResponse> listarPorCategoria(Long categoriaId, int pagina, int tamanho) {
        Page<Documentario> page = documentarioRepository.findByCategoriaId(
                categoriaId,
                PageRequest.of(pagina, tamanho, Sort.by("dataCriacao").descending())
        );
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentarioResponse> listarMeus(Utilizador utilizador, int pagina, int tamanho) {
        Page<Documentario> page = documentarioRepository.findByUtilizador(
                utilizador,
                PageRequest.of(pagina, tamanho, Sort.by("dataCriacao").descending())
        );
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public DocumentarioResponse buscarPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public DocumentarioResponse actualizar(Long id, DocumentarioRequest request, Utilizador utilizador) {
        Documentario doc = buscarEntidade(id);
        verificarPropriedade(doc, utilizador);

        doc.setTitulo(request.getTitulo());
        doc.setDescricao(request.getDescricao());
        doc.setAno(request.getAno());

        if (request.getCategoriaId() != null) {
            doc.setCategoria(categoriaService.buscarEntidade(request.getCategoriaId()));
        }

        return toResponse(documentarioRepository.save(doc));
    }

    @Transactional
    public void eliminar(Long id, Utilizador utilizador, String ip) {
        Documentario doc = buscarEntidade(id);
        verificarPropriedade(doc, utilizador);

        fileStorageUtil.eliminar(doc.getCaminhoOriginal());
        fileStorageUtil.eliminar(doc.getCaminhoComprimido());
        fileStorageUtil.eliminar(doc.getCaminhoThumbnail());

        documentarioRepository.delete(doc);
        logService.registar(AcaoLog.DELETE_DOCUMENTARIO, "Eliminado: " + doc.getTitulo(), utilizador, ip);
    }

    @Transactional
    public CompressaoRelatorioResponse gerarRelatorioCompressao(Long id) {
        Documentario doc = buscarEntidade(id);
        if (doc.getStatus() == StatusDocumentario.PENDENTE || doc.getStatus() == StatusDocumentario.PROCESSANDO) {
            throw new NegocioException("O documentário ainda está a ser processado. Estado: " + doc.getStatus());
        }
        return compressaoService.gerarRelatorio(doc);
    }

    @Transactional
    public void incrementarVisualizacoes(Long id) {
        documentarioRepository.incrementarVisualizacoes(id);
    }

    @Transactional
    public void incrementarDownloads(Long id, Utilizador utilizador, String ip) {
        documentarioRepository.incrementarDownloads(id);
        Documentario doc = buscarEntidade(id);
        logService.registar(AcaoLog.DOWNLOAD, "Download: " + doc.getTitulo(), utilizador, ip);
    }

    @Transactional(readOnly = true)
    public List<DocumentarioResponse> listarMaisVistos() {
        return documentarioRepository.findTop10ByStatusOrderByVisualizacoesDesc(StatusDocumentario.PRONTO)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void recomprimir(Long id, Utilizador admin, String ip) {
        Documentario doc = buscarEntidade(id);
        if (!fileStorageUtil.existe(doc.getCaminhoOriginal())) {
            throw new NegocioException("Ficheiro original não encontrado no disco");
        }
        doc.setStatus(StatusDocumentario.PENDENTE);
        documentarioRepository.save(doc);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                compressaoService.comprimirAsync(id);
            }
        });
        logService.registar(AcaoLog.COMPRESSAO, "Recompressão iniciada para: " + doc.getTitulo(), admin, ip);
    }

    public Documentario buscarEntidade(Long id) {
        return documentarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documentário não encontrado: " + id));
    }

    private void verificarPropriedade(Documentario doc, Utilizador utilizador) {
        boolean ehAdmin = utilizador.getRole() == Role.ADMIN;
        boolean ehDono = doc.getUtilizador().getId().equals(utilizador.getId());
        if (!ehAdmin && !ehDono) {
            throw new NegocioException("Não tem permissão para modificar este documentário");
        }
    }

    private void validarFicheiroVideo(MultipartFile ficheiro) {
        if (ficheiro.isEmpty()) {
            throw new NegocioException("O ficheiro está vazio");
        }
        String contentType = ficheiro.getContentType();
        if (contentType == null || (!contentType.startsWith("video/") && !contentType.equals("application/octet-stream"))) {
            if (contentType != null && !List.of("video/mp4", "video/avi", "video/mkv",
                    "video/x-msvideo", "video/quicktime", "video/x-matroska", "video/webm").contains(contentType)) {
                log.warn("Tipo de conteúdo inesperado: {}. Aceitando mesmo assim.", contentType);
            }
        }
    }

    private DocumentarioResponse toResponse(Documentario doc) {
        String urlStreaming = BASE_URL + "/api/streaming/" + doc.getId();
        String urlDownload = BASE_URL + "/api/documentarios/" + doc.getId() + "/download";
        String urlThumbnail = fileStorageUtil.existe(doc.getCaminhoThumbnail())
                ? BASE_URL + "/api/streaming/" + doc.getId() + "/thumbnail"
                : null;

        return DocumentarioResponse.builder()
                .id(doc.getId())
                .titulo(doc.getTitulo())
                .descricao(doc.getDescricao())
                .ano(doc.getAno())
                .duracaoSegundos(doc.getDuracaoSegundos())
                .formato(doc.getFormato())
                .status(doc.getStatus())
                .categoria(doc.getCategoria() != null ? new com.arquivodigital.dto.response.CategoriaResponse(
                        doc.getCategoria().getId(), doc.getCategoria().getNome(), doc.getCategoria().getDescricao()
                ) : null)
                .nomeUtilizador(doc.getUtilizador() != null ? doc.getUtilizador().getNome() : null)
                .urlStreaming(urlStreaming)
                .urlDownload(urlDownload)
                .urlThumbnail(urlThumbnail)
                .tamanhoOriginalBytes(doc.getTamanhoOriginalBytes())
                .tamanhoComprimidoBytes(doc.getTamanhoComprimidoBytes())
                .taxaCompressao(doc.getTaxaCompressao())
                .tempoProcessamentoMs(doc.getTempoProcessamentoMs())
                .codecVideo(doc.getCodecVideo())
                .codecAudio(doc.getCodecAudio())
                .visualizacoes(doc.getVisualizacoes())
                .downloads(doc.getDownloads())
                .dataCriacao(doc.getDataCriacao())
                .dataAtualizacao(doc.getDataAtualizacao())
                .build();
    }

    private PageResponse<DocumentarioResponse> toPageResponse(Page<Documentario> page) {
        return PageResponse.<DocumentarioResponse>builder()
                .conteudo(page.getContent().stream().map(this::toResponse).toList())
                .paginaActual(page.getNumber())
                .totalPaginas(page.getTotalPages())
                .totalElementos(page.getTotalElements())
                .tamanhoPagina(page.getSize())
                .primeira(page.isFirst())
                .ultima(page.isLast())
                .build();
    }
}
