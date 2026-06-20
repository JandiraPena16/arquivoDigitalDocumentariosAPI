package com.arquivodigital.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Arquivo Digital de Documentários — API",
                version = "1.0.0",
                description = """
                        API RESTful para a Plataforma de Arquivo Digital de Documentários.

                        **Grupo 06 — Projecto Final Multimédia 2026**

                        ## Como usar
                        1. Registe-se em `POST /api/auth/registar` ou faça login em `POST /api/auth/login`
                        2. Copie o `token` da resposta
                        3. Clique em **Authorize** (cadeado) e cole o token no campo **Value** como: `Bearer <token>`
                        4. Todos os endpoints protegidos passarão a funcionar
                        """,
                contact = @Contact(name = "Grupo 06", email = "grupo06@arquivo.ao")
        ),
        servers = @Server(url = "http://localhost:8080", description = "Servidor Local")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Introduza o token JWT obtido em /api/auth/login. Formato: Bearer <token>"
)
public class OpenApiConfig {
}
