package com.arquivodigital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Resposta de erro da API")
public class ErroResponse {
    private int status;
    private String erro;
    private String mensagem;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, String> detalhes;
}
