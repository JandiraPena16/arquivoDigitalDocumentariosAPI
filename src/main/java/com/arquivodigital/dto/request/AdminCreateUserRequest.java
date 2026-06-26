package com.arquivodigital.dto.request;

import com.arquivodigital.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminCreateUserRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100)
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Password é obrigatória")
    @Size(min = 6, message = "A password deve ter pelo menos 6 caracteres")
    private String password;

    private Role role;   // USER (default) ou ADMIN
}
