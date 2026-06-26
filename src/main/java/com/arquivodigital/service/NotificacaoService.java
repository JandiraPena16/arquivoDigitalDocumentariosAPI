package com.arquivodigital.service;

import com.arquivodigital.dto.response.NotificacaoResponse;
import com.arquivodigital.entity.Documentario;
import com.arquivodigital.entity.Notificacao;
import com.arquivodigital.entity.NotificacaoTipo;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.repository.AvaliacaoRepository;
import com.arquivodigital.repository.NotificacaoRepository;
import com.arquivodigital.repository.UtilizadorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoService {

    private final NotificacaoRepository repo;
    private final AvaliacaoRepository avaliacaoRepository;
    private final UtilizadorRepository utilizadorRepository;

    // ===== Criação (triggers) =====

    @Transactional
    public void notificarUploadConcluido(Documentario doc) {
        if (doc.getUtilizador() == null) return;
        criar(doc.getUtilizador(), NotificacaoTipo.UPLOAD_CONCLUIDO,
                "Upload concluído",
                "O teu documentário \"" + doc.getTitulo() + "\" está pronto",
                String.valueOf(doc.getId()));
    }

    @Transactional
    public void notificarLikeRecebido(Documentario doc, Utilizador quemGostou) {
        if (doc.getUtilizador() == null || quemGostou == null) return;
        if (doc.getUtilizador().getId().equals(quemGostou.getId())) return; // não a si próprio
        criar(doc.getUtilizador(), NotificacaoTipo.LIKE_RECEBIDO,
                "Novo gosto no teu vídeo",
                quemGostou.getNome() + " gostou de \"" + doc.getTitulo() + "\"",
                String.valueOf(doc.getId()));
    }

    @Transactional
    public void notificarDownloadRecebido(Documentario doc, Utilizador quemDescarregou) {
        if (doc.getUtilizador() == null || quemDescarregou == null) return;
        if (doc.getUtilizador().getId().equals(quemDescarregou.getId())) return; // não a si próprio
        criar(doc.getUtilizador(), NotificacaoTipo.DOWNLOAD_RECEBIDO,
                "Novo download do teu vídeo",
                quemDescarregou.getNome() + " descarregou \"" + doc.getTitulo() + "\"",
                String.valueOf(doc.getId()));
    }

    @Transactional
    public void notificarNovoVideoNaCategoria(Documentario doc) {
        if (doc.getCategoria() == null) return;
        List<Utilizador> interessados =
                avaliacaoRepository.findUtilizadoresQueGostaramDaCategoria(doc.getCategoria().getId());
        for (Utilizador u : interessados) {
            if (doc.getUtilizador() != null && u.getId().equals(doc.getUtilizador().getId())) continue;
            criar(u, NotificacaoTipo.NOVO_VIDEO,
                    "Novo documentário disponível",
                    "\"" + doc.getTitulo() + "\" foi adicionado em " + doc.getCategoria().getNome(),
                    String.valueOf(doc.getId()));
        }
    }

    @Transactional
    public void notificarLive(Long broadcasterUserId, String nomeBroadcaster, String liveId, String titulo) {
        for (Utilizador u : utilizadorRepository.findAll()) {
            if (broadcasterUserId != null && u.getId().equals(broadcasterUserId)) continue;
            criar(u, NotificacaoTipo.LIVE,
                    "Transmissão ao vivo",
                    (nomeBroadcaster != null ? nomeBroadcaster : "Alguém") + " começou: \"" + titulo + "\"",
                    liveId);
        }
    }

    private void criar(Utilizador dest, NotificacaoTipo tipo, String titulo, String msg, String ref) {
        repo.save(Notificacao.builder()
                .utilizador(dest).tipo(tipo).titulo(titulo).mensagem(msg)
                .referencia(ref).lida(false).build());
    }

    // ===== Consulta =====

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> listar(Utilizador u) {
        return repo.findByUtilizadorOrderByDataCriacaoDesc(u, PageRequest.of(0, 50))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Utilizador u) {
        return repo.countByUtilizadorAndLidaFalse(u);
    }

    @Transactional
    public void marcarTodasLidas(Utilizador u) {
        repo.marcarTodasLidas(u);
    }

    private NotificacaoResponse toResponse(Notificacao n) {
        return NotificacaoResponse.builder()
                .id(n.getId())
                .tipo(n.getTipo().name())
                .titulo(n.getTitulo())
                .mensagem(n.getMensagem())
                .referencia(n.getReferencia())
                .lida(n.isLida())
                .dataCriacao(n.getDataCriacao())
                .build();
    }
}
