package com.arquivodigital.signaling;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa uma transmissão ao vivo em curso (mantida em memória).
 * O vídeo flui P2P (WebRTC) entre emissor e espectadores — o servidor
 * apenas faz a sinalização e mantém este registo para listagem.
 */
@Getter
@Setter
public class LiveSession {

    private final String liveId;
    private final String broadcasterSessionId;
    private final Long broadcasterUserId;
    private final String broadcasterNome;
    private final String titulo;
    private final Instant iniciadaEm;

    /** sessionIds (WebSocket) dos espectadores ligados. */
    private final Set<String> espectadores = ConcurrentHashMap.newKeySet();

    /** Reação de cada utilizador: userId -> "like" | "dislike" (uma por pessoa). */
    private final Map<Long, String> reacoes = new ConcurrentHashMap<>();

    /**
     * Aplica a reação de um utilizador com toggle:
     * - mesma reação outra vez -> remove (anula)
     * - reação diferente -> troca (ex.: like passa a dislike)
     * - sem reação -> adiciona
     */
    public void aplicarReacao(Long userId, String tipo) {
        if (userId == null || tipo == null) return;
        String atual = reacoes.get(userId);
        if (tipo.equals(atual)) {
            reacoes.remove(userId);
        } else {
            reacoes.put(userId, tipo);
        }
    }

    public int getLikes() {
        return (int) reacoes.values().stream().filter("like"::equals).count();
    }

    public int getDislikes() {
        return (int) reacoes.values().stream().filter("dislike"::equals).count();
    }

    public LiveSession(String liveId, String broadcasterSessionId, Long broadcasterUserId,
                       String broadcasterNome, String titulo) {
        this.liveId = liveId;
        this.broadcasterSessionId = broadcasterSessionId;
        this.broadcasterUserId = broadcasterUserId;
        this.broadcasterNome = broadcasterNome;
        this.titulo = titulo;
        this.iniciadaEm = Instant.now();
    }

    public int getNumEspectadores() {
        return espectadores.size();
    }
}
