package com.arquivodigital.signaling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servidor de sinalização WebRTC.
 *
 * O servidor NÃO transporta vídeo — apenas faz a ponte das mensagens de
 * sinalização (SDP offer/answer e ICE candidates) entre o emissor (broadcaster)
 * e cada espectador (viewer). O vídeo flui P2P directamente entre os pares.
 *
 * Topologia: malha (mesh) — o emissor mantém uma ligação P2P por espectador.
 * Adequado para um número reduzido de espectadores (sem media server/SFU).
 *
 * Protocolo (JSON) cliente -> servidor:
 *   {type:"start",  titulo}                  emissor inicia uma live
 *   {type:"join",   liveId}                  espectador entra numa live
 *   {type:"offer",  target, sdp}             SDP offer para um par
 *   {type:"answer", target, sdp}             SDP answer para um par
 *   {type:"ice",    target, candidate}       ICE candidate para um par
 *   {type:"stop"}                            emissor termina a live
 *   {type:"leave"}                           espectador sai
 *
 * servidor -> cliente:
 *   {type:"started",     liveId, selfId}
 *   {type:"joined",      selfId}
 *   {type:"viewer-join", viewerId}           (para o emissor)
 *   {type:"viewer-leave",viewerId}           (para o emissor)
 *   {type:"offer/answer/ice", from, ...}     mensagens reencaminhadas
 *   {type:"live-ended"}                      (para os espectadores)
 *   {type:"error", mensagem}
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SignalingHandler extends TextWebSocketHandler {

    private final LiveRegistry registry;
    private final com.arquivodigital.service.NotificacaoService notificacaoService;
    private final ObjectMapper mapper = new ObjectMapper();

    /** sessionId -> sessão WebSocket activa. */
    private final Map<String, WebSocketSession> sessoes = new ConcurrentHashMap<>();
    /** sessionId de espectador -> liveId a que pertence. */
    private final Map<String, String> espectadorParaLive = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessoes.put(session.getId(), session);
        log.info("WS ligado: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode msg = mapper.readTree(message.getPayload());
            String type = texto(msg, "type");
            if (type == null) return;

            switch (type) {
                case "start"  -> aoIniciar(session, msg);
                case "join"   -> aoEntrar(session, msg);
                case "offer", "answer", "ice" -> reencaminhar(session, msg, type);
                case "chat"     -> aoChat(session, msg);
                case "reaction" -> aoReacao(session, msg);
                case "stop"   -> aoTerminar(session);
                case "leave"  -> aoSair(session);
                default -> log.warn("Tipo de mensagem desconhecido: {}", type);
            }
        } catch (Exception e) {
            log.warn("Erro ao tratar mensagem de sinalização: {}", e.getMessage());
        }
    }

    private void aoIniciar(WebSocketSession session, JsonNode msg) {
        Long userId = (Long) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_USER_ID);
        String nome = (String) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_NOME);
        String titulo = texto(msg, "titulo");
        if (titulo == null || titulo.isBlank()) titulo = "Transmissão ao vivo";

        String liveId = UUID.randomUUID().toString();
        registry.criar(liveId, session.getId(), userId, nome, titulo);

        ObjectNode resp = mapper.createObjectNode();
        resp.put("type", "started");
        resp.put("liveId", liveId);
        resp.put("selfId", session.getId());
        enviar(session, resp);
        log.info("Live iniciada {} por user {} ({})", liveId, userId, nome);

        // Notificar os outros utilizadores de que começou uma live
        try {
            notificacaoService.notificarLive(userId, nome, liveId, titulo);
        } catch (Exception e) {
            log.warn("Falha ao notificar live {}: {}", liveId, e.getMessage());
        }
    }

    private void aoEntrar(WebSocketSession session, JsonNode msg) {
        String liveId = texto(msg, "liveId");
        LiveSession live = registry.porId(liveId).orElse(null);
        if (live == null) {
            erro(session, "Live não encontrada ou já terminada");
            return;
        }
        live.getEspectadores().add(session.getId());
        espectadorParaLive.put(session.getId(), liveId);

        ObjectNode resp = mapper.createObjectNode();
        resp.put("type", "joined");
        resp.put("selfId", session.getId());
        enviar(session, resp);

        // Envia as contagens de reações actuais ao novo espectador
        ObjectNode counts = mapper.createObjectNode();
        counts.put("type", "reaction");
        counts.put("likes", live.getLikes());
        counts.put("dislikes", live.getDislikes());
        enviar(session, counts);

        // Notifica o emissor para criar a ligação P2P com este espectador
        WebSocketSession broadcaster = sessoes.get(live.getBroadcasterSessionId());
        if (broadcaster != null) {
            ObjectNode notif = mapper.createObjectNode();
            notif.put("type", "viewer-join");
            notif.put("viewerId", session.getId());
            enviar(broadcaster, notif);
        }
        log.info("Espectador {} entrou na live {}", session.getId(), liveId);
    }

    /** Reencaminha offer/answer/ice para o par-alvo, carimbando o remetente em "from". */
    private void reencaminhar(WebSocketSession session, JsonNode msg, String type) {
        String target = texto(msg, "target");
        if (target == null) return;
        WebSocketSession destino = sessoes.get(target);
        if (destino == null) return;

        ObjectNode out = mapper.createObjectNode();
        out.put("type", type);
        out.put("from", session.getId());
        if (msg.has("sdp")) out.set("sdp", msg.get("sdp"));
        if (msg.has("candidate")) out.set("candidate", msg.get("candidate"));
        enviar(destino, out);
    }

    /** Mensagem de chat — difundida a todos os participantes da live. */
    private void aoChat(WebSocketSession session, JsonNode msg) {
        String texto = texto(msg, "texto");
        if (texto == null || texto.isBlank()) return;
        LiveSession live = liveDaSessao(session);
        if (live == null) return;

        ObjectNode out = mapper.createObjectNode();
        out.put("type", "chat");
        out.put("nome", nomeApresentacao(session));
        out.put("texto", texto.length() > 500 ? texto.substring(0, 500) : texto);
        difundir(live, out);
    }

    /**
     * Reação (like/dislike) — uma por utilizador, com toggle.
     * Clicar de novo na mesma reação anula; clicar na oposta troca o voto.
     */
    private void aoReacao(WebSocketSession session, JsonNode msg) {
        String tipo = texto(msg, "tipo");
        LiveSession live = liveDaSessao(session);
        if (live == null || (!"like".equals(tipo) && !"dislike".equals(tipo))) return;

        Long userId = (Long) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_USER_ID);
        if (userId == null) return;

        live.aplicarReacao(userId, tipo);

        ObjectNode out = mapper.createObjectNode();
        out.put("type", "reaction");
        out.put("likes", live.getLikes());
        out.put("dislikes", live.getDislikes());
        difundir(live, out);
    }

    /** Descobre a que live pertence uma sessão (emissor ou espectador). */
    private LiveSession liveDaSessao(WebSocketSession session) {
        LiveSession live = registry.porBroadcasterSession(session.getId()).orElse(null);
        if (live != null) return live;
        String liveId = espectadorParaLive.get(session.getId());
        return liveId != null ? registry.porId(liveId).orElse(null) : null;
    }

    /** Envia uma mensagem a todos os participantes (emissor + espectadores). */
    private void difundir(LiveSession live, ObjectNode payload) {
        WebSocketSession broadcaster = sessoes.get(live.getBroadcasterSessionId());
        if (broadcaster != null) enviar(broadcaster, payload);
        for (String viewerId : live.getEspectadores()) {
            WebSocketSession v = sessoes.get(viewerId);
            if (v != null) enviar(v, payload);
        }
    }

    /** Nome curto para apresentação (parte antes do @ do email). */
    private String nomeApresentacao(WebSocketSession session) {
        String nome = (String) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_NOME);
        if (nome == null) return "Anónimo";
        int at = nome.indexOf('@');
        return at > 0 ? nome.substring(0, at) : nome;
    }

    private void aoTerminar(WebSocketSession session) {
        registry.porBroadcasterSession(session.getId()).ifPresent(live -> {
            ObjectNode fim = mapper.createObjectNode();
            fim.put("type", "live-ended");
            for (String viewerId : live.getEspectadores()) {
                WebSocketSession v = sessoes.get(viewerId);
                if (v != null) enviar(v, fim);
                espectadorParaLive.remove(viewerId);
            }
            registry.remover(live.getLiveId());
            log.info("Live {} terminada pelo emissor", live.getLiveId());
        });
    }

    private void aoSair(WebSocketSession session) {
        String liveId = espectadorParaLive.remove(session.getId());
        if (liveId == null) return;
        registry.porId(liveId).ifPresent(live -> {
            live.getEspectadores().remove(session.getId());
            WebSocketSession broadcaster = sessoes.get(live.getBroadcasterSessionId());
            if (broadcaster != null) {
                ObjectNode notif = mapper.createObjectNode();
                notif.put("type", "viewer-leave");
                notif.put("viewerId", session.getId());
                enviar(broadcaster, notif);
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessoes.remove(session.getId());
        // Se era emissor, termina a live; se era espectador, notifica saída.
        aoTerminar(session);
        aoSair(session);
        log.info("WS desligado: {}", session.getId());
    }

    // ===== helpers =====

    private void enviar(WebSocketSession session, ObjectNode payload) {
        if (session == null || !session.isOpen()) return;
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
            }
        } catch (IOException e) {
            log.warn("Falha ao enviar para {}: {}", session.getId(), e.getMessage());
        }
    }

    private void erro(WebSocketSession session, String mensagem) {
        ObjectNode err = mapper.createObjectNode();
        err.put("type", "error");
        err.put("mensagem", mensagem);
        enviar(session, err);
    }

    private String texto(JsonNode node, String campo) {
        return node.has(campo) && !node.get(campo).isNull() ? node.get(campo).asText() : null;
    }
}
