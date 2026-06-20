package com.arquivodigital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Resposta de autenticação com JWT e ID de sessão")
public class AuthResponse {

    @Schema(description = "JWT Bearer Token")
    private String token;

    @Schema(description = "Tipo do token", example = "Bearer")
    private String tipo;

    @Schema(description = "ID único da sessão (pode ser usado para revogar a sessão)")
    private String sessaoId;

    @Schema(description = "Dados do utilizador autenticado")
    private UtilizadorResponse utilizador;
}
