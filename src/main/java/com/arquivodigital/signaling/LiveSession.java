package com.arquivodigital.signaling;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
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
