package com.arquivodigital.signaling;

import com.arquivodigital.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Valida o JWT (passado como query param ?token=) antes de abrir o WebSocket
 * e coloca o userId e o nome nos atributos da sessão.
 */
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_NOME = "nome";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extrairToken(request);
        if (token == null || !jwtUtil.isTokenValido(token)) {
            log.warn("Handshake WebSocket rejeitado: token em falta ou inválido");
            return false;
        }
        try {
            Claims claims = jwtUtil.extrairClaims(token);
            Long userId = claims.get("userId", Number.class).longValue();
            String email = claims.getSubject();
            attributes.put(ATTR_USER_ID, userId);
            attributes.put(ATTR_NOME, email);
            return true;
        } catch (Exception e) {
            log.warn("Erro ao processar token no handshake: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // nada a fazer
    }

    private String extrairToken(ServerHttpRequest request) {
        List<String> tokens = UriComponentsBuilder.fromUri(request.getURI())
                .build().getQueryParams().get("token");
        if (tokens != null && !tokens.isEmpty()) {
            return tokens.get(0);
        }
        return null;
    }
}
