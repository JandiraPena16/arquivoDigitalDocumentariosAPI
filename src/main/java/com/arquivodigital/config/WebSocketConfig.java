package com.arquivodigital.config;

import com.arquivodigital.security.JwtUtil;
import com.arquivodigital.signaling.JwtHandshakeInterceptor;
import com.arquivodigital.signaling.SignalingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final SignalingHandler signalingHandler;
    private final JwtUtil jwtUtil;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingHandler, "/ws/signaling")
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtil))
                .setAllowedOrigins("*");
    }
}
