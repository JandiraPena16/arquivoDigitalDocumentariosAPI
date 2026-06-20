package com.arquivodigital.controller;

import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.security.UserDetailsImpl;
import com.arquivodigital.service.StreamingService;
import com.arquivodigital.service.UtilizadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/streaming")
@RequiredArgsConstructor
@Tag(name = "Streaming", description = "Streaming de vídeo com suporte a Range Requests (HTTP 206)")
public class StreamingController {

    private final StreamingService streamingService;
    private final UtilizadorService utilizadorService;

    @GetMapping("/{id}")
    @Operation(
            summary = "Stream de vídeo (suporta Range Requests para seek e resumo)",
            description = """
                    Endpoint de streaming compatível com players de vídeo Android/Web.

                    - **Sem Range header**: devolve o ficheiro completo (HTTP 200)
                    - **Com Range header**: devolve chunk parcial (HTTP 206) — necessário para seek no vídeo

                    **Exemplo de uso com Range:**
                    ```
                    Range: bytes=0-1048575
                    ```

                    O utilizador autenticado tem os streams registados nos logs.
                    Utilizadores não autenticados também podem fazer stream.
                    """
    )
    public ResponseEntity<Resource> stream(
            @PathVariable Long id,
            @Parameter(description = "Range de bytes (ex: bytes=0-1048575)")
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            @AuthenticationPrincipal UserDetailsImpl principal,
            HttpServletRequest httpRequest
    ) {
        Utilizador utilizador = principal != null
                ? utilizadorService.buscarEntidade(principal.getId())
                : null;

        return streamingService.stream(id, rangeHeader, utilizador, httpRequest.getRemoteAddr());
    }

    @GetMapping("/{id}/thumbnail")
    @Operation(summary = "Obter thumbnail do documentário")
    public ResponseEntity<Resource> thumbnail(@PathVariable Long id) {
        return streamingService.thumbnail(id);
    }
}
