package com.arquivodigital.dto.response;

import com.arquivodigital.entity.AcaoLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Registo de log de actividade")
public class LogResponse {
    private Long id;
    private AcaoLog acao;
    private String detalhe;
    private String nomeUtilizador;
    private Long utilizadorId;
    private String ip;
    private LocalDateTime timestamp;
}
