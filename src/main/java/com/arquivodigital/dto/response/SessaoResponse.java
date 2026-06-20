package com.arquivodigital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Informação de uma sessão activa")
public class SessaoResponse {
    private String id;
    private String ipOrigem;
    private String userAgent;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataExpiracao;
    private boolean ativa;
}
