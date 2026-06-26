package com.arquivodigital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Uma notificação do utilizador")
public class NotificacaoResponse {
    private Long id;
    private String tipo;        // NOVO_VIDEO | UPLOAD_CONCLUIDO | LIKE_RECEBIDO | LIVE
    private String titulo;
    private String mensagem;
    private String referencia;  // docId ou liveId, conforme o tipo
    private boolean lida;
    private LocalDateTime dataCriacao;
}
