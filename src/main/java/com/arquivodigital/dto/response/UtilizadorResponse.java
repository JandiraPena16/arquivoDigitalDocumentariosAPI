package com.arquivodigital.dto.response;

import com.arquivodigital.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Dados de um utilizador")
public class UtilizadorResponse {
    private Long id;
    private String nome;
    private String email;
    private Role role;
    private boolean ativo;
    private LocalDateTime dataCriacao;
}
