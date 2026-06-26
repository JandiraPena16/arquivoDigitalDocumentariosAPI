package com.arquivodigital.service;

import com.arquivodigital.dto.response.AtividadeItem;
import com.arquivodigital.dto.response.UtilizadorAtividadeResponse;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.repository.AvaliacaoRepository;
import com.arquivodigital.repository.HistoricoRepository;
import com.arquivodigital.repository.MinhaListaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AtividadeService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final MinhaListaRepository minhaListaRepository;
    private final HistoricoRepository historicoRepository;
    private final UtilizadorService utilizadorService;

    @Transactional(readOnly = true)
    public UtilizadorAtividadeResponse atividade(Long userId) {
        Utilizador u = utilizadorService.buscarEntidade(userId);

        List<AtividadeItem> avaliacoes = avaliacaoRepository.findByUtilizadorOrderByDataAvaliacaoDesc(u).stream()
                .map(a -> new AtividadeItem(
                        a.getDocumentario().getId(), a.getDocumentario().getTitulo(),
                        a.getValor(), a.getDataAvaliacao()))
                .toList();

        List<AtividadeItem> lista = minhaListaRepository.findByUtilizadorOrderByDataAdicionadoDesc(u).stream()
                .map(m -> new AtividadeItem(
                        m.getDocumentario().getId(), m.getDocumentario().getTitulo(),
                        null, m.getDataAdicionado()))
                .toList();

        List<AtividadeItem> historico = historicoRepository.findByUtilizadorOrderByDataVistoDesc(u).stream()
                .map(h -> new AtividadeItem(
                        h.getDocumentario().getId(), h.getDocumentario().getTitulo(),
                        null, h.getDataVisto()))
                .toList();

        return new UtilizadorAtividadeResponse(avaliacoes, lista, historico);
    }
}
