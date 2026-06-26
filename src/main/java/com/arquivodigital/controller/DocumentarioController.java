package com.arquivodigital.controller;

import com.arquivodigital.dto.request.DocumentarioRequest;
import com.arquivodigital.dto.response.CompressaoRelatorioResponse;
import com.arquivodigital.dto.response.DocumentarioResponse;
import com.arquivodigital.dto.response.PageResponse;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.exception.custom.NegocioException;
import com.arquivodigital.security.UserDetailsImpl;
import jakarta.validation.Valid;
import com.arquivodigital.service.DocumentarioService;
import com.arquivodigital.service.UtilizadorService;
import com.arquivodigital.util.FileStorageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documentarios")
@RequiredArgsConstructor
@Tag(name = "Documentários", description = "Upload, pesquisa, download e gestão de documentários")
public class DocumentarioController {

    private final DocumentarioService documentarioService;
    private final UtilizadorService utilizadorService;
    private final FileStorageUtil fileStorageUtil;

    @GetMapping
    @Operation(summary = "Listar documentários prontos (paginado, público)")
    public ResponseEntity<PageResponse<DocumentarioResponse>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho
    ) {
        return ResponseEntity.ok(documentarioService.listar(pagina, tamanho));
    }

    @GetMapping("/pesquisa")
    @Operation(summary = "Pesquisar documentários por título, categoria ou ano (público)")
    public ResponseEntity<PageResponse<DocumentarioResponse>> pesquisar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Integer ano,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho
    ) {
        return ResponseEntity.ok(documentarioService.pesquisar(q, categoriaId, ano, pagina, tamanho));
    }

    @GetMapping("/mais-vistos")
    @Operation(summary = "Top 10 documentários mais vistos (público)")
    public ResponseEntity<List<DocumentarioResponse>> maisVistos() {
        return ResponseEntity.ok(documentarioService.listarMaisVistos());
    }

    @GetMapping("/categoria/{categoriaId}")
    @Operation(summary = "Listar documentários por categoria (público)")
    public ResponseEntity<PageResponse<DocumentarioResponse>> porCategoria(
            @PathVariable Long categoriaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho
    ) {
        return ResponseEntity.ok(documentarioService.listarPorCategoria(categoriaId, pagina, tamanho));
    }

    @GetMapping("/meus")
    @Operation(summary = "Listar os meus documentários",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<DocumentarioResponse>> meus(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        return ResponseEntity.ok(documentarioService.listarMeus(utilizador, pagina, tamanho));
    }

    @GetMapping("/admin/todos")
    @Operation(summary = "Listar TODOS os documentários, qualquer estado (apenas ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<DocumentarioResponse>> listarTodosAdmin(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho
    ) {
        return ResponseEntity.ok(documentarioService.listarTodosAdmin(pagina, tamanho));
    }

    @GetMapping("/admin/estatisticas")
    @Operation(summary = "Estatísticas globais: compressão poupada, por estado e por categoria (apenas ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.arquivodigital.dto.response.EstatisticasResponse> estatisticas() {
        return ResponseEntity.ok(documentarioService.estatisticas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar documentário por ID (público)")
    public ResponseEntity<DocumentarioResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(documentarioService.buscarPorId(id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Fazer upload de um documentário (autenticado)",
            description = "Enviar como multipart/form-data. Preenche cada campo individualmente.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentarioResponse> upload(
            @Parameter(description = "Ficheiro de vídeo") @RequestPart("ficheiro") MultipartFile ficheiro,
            @Parameter(description = "Título do documentário (obrigatório)") @RequestParam("titulo") String titulo,
            @Parameter(description = "Descrição (opcional)") @RequestParam(value = "descricao", required = false) String descricao,
            @Parameter(description = "Ano de produção (opcional)") @RequestParam(value = "ano", required = false) Integer ano,
            @Parameter(description = "ID da categoria (opcional)") @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        if (titulo == null || titulo.isBlank()) {
            throw new NegocioException("Título é obrigatório");
        }
        DocumentarioRequest request = new DocumentarioRequest();
        request.setTitulo(titulo);
        request.setDescricao(descricao);
        request.setAno(ano);
        request.setCategoriaId(categoriaId);
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        DocumentarioResponse response = documentarioService.upload(ficheiro, request, utilizador, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar metadados de um documentário (dono ou ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentarioResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DocumentarioRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        return ResponseEntity.ok(documentarioService.actualizar(id, request, utilizador));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar documentário (dono ou ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        documentarioService.eliminar(id, utilizador, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download do documentário (autenticado)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        var doc = documentarioService.buscarEntidade(id);

        String caminho = doc.getCaminhoComprimido() != null ? doc.getCaminhoComprimido() : doc.getCaminhoOriginal();

        documentarioService.incrementarDownloads(id, utilizador, httpRequest.getRemoteAddr());

        Resource resource = new FileSystemResource(caminho);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getTitulo() + ".mp4\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/{id}/relatorio-compressao")
    @Operation(summary = "Relatório comparativo de compressão (público após processamento)")
    public ResponseEntity<CompressaoRelatorioResponse> relatorioCompressao(@PathVariable Long id) {
        return ResponseEntity.ok(documentarioService.gerarRelatorioCompressao(id));
    }

    @PostMapping("/{id}/recomprimir")
    @Operation(summary = "Recomprimir documentário (apenas ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> recomprimir(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        Utilizador admin = utilizadorService.buscarEntidade(principal.getId());
        documentarioService.recomprimir(id, admin, httpRequest.getRemoteAddr());
        return ResponseEntity.accepted().body(Map.of("mensagem", "Recompressão iniciada. Verifique o estado em GET /api/documentarios/" + id));
    }

    @PostMapping(value = "/{id}/atualizar-capa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Atualizar a capa (thumbnail) do documentário (dono ou ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentarioResponse> atualizarCapa(
            @PathVariable Long id,
            @RequestPart("imagem") MultipartFile imagem,
            @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        return ResponseEntity.ok(documentarioService.atualizarCapa(id, imagem, utilizador));
    }

    @PostMapping(value = "/{id}/substituir-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Substituir o ficheiro de vídeo (dono ou ADMIN) — reinicia processamento",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentarioResponse> substituirVideo(
            @PathVariable Long id,
            @RequestPart("ficheiro") MultipartFile ficheiro,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        Utilizador utilizador = utilizadorService.buscarEntidade(principal.getId());
        return ResponseEntity.ok(documentarioService.substituirVideo(id, ficheiro, utilizador, httpRequest.getRemoteAddr()));
    }
}
