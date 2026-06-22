package com.arquivodigital.signaling;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Registo em memória das lives activas. Partilhado entre o handler de
 * sinalização (WebSocket) e o controller REST (listagem).
 */
@Component
public class LiveRegistry {

    private final Map<String, LiveSession> lives = new ConcurrentHashMap<>();
    /** sessionId do emissor -> liveId, para limpeza rápida na desconexão. */
    private final Map<String, String> broadcasterToLive = new ConcurrentHashMap<>();

    public LiveSession criar(String liveId, String broadcasterSessionId, Long userId, String nome, String titulo) {
        LiveSession s = new LiveSession(liveId, broadcasterSessionId, userId, nome, titulo);
        lives.put(liveId, s);
        broadcasterToLive.put(broadcasterSessionId, liveId);
        return s;
    }

    public Optional<LiveSession> porId(String liveId) {
        return Optional.ofNullable(lives.get(liveId));
    }

    public Optional<LiveSession> porBroadcasterSession(String sessionId) {
        String liveId = broadcasterToLive.get(sessionId);
        return liveId != null ? porId(liveId) : Optional.empty();
    }

    public Collection<LiveSession> activas() {
        return lives.values();
    }

    public void remover(String liveId) {
        LiveSession s = lives.remove(liveId);
        if (s != null) {
            broadcasterToLive.remove(s.getBroadcasterSessionId());
        }
    }

    public boolean existe(String liveId) {
        return lives.containsKey(liveId);
    }
}
