package com.arquivodigital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Uma transmissão ao vivo activa")
public class LiveResponse {
    private String id;
    private String titulo;
    private String nomeBroadcaster;
    private Long broadcasterUserId;
    private int numEspectadores;
    private Instant iniciadaEm;
}
