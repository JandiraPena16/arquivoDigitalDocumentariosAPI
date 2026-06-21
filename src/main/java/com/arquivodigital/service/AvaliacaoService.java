package com.arquivodigital.service;

import com.arquivodigital.dto.request.AvaliacaoRequest;
import com.arquivodigital.dto.response.AvaliacaoResponse;
import com.arquivodigital.entity.Avaliacao;
import com.arquivodigital.entity.Documentario;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.exception.custom.ResourceNotFoundException;
import com.arquivodigital.repository.AvaliacaoRepository;
import com.arquivodigital.repository.DocumentarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final DocumentarioRepository documentarioRepository;

    @Transactional(readOnly = true)
    public AvaliacaoResponse obter(Long docId, Utilizador utilizador) {
        Documentario doc = documentarioRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Documentário não encontrado: " + docId));

        Optional<Avaliacao> avaliacao = avaliacaoRepository.findByUtilizadorAndDocumentario(utilizador, doc);
        return AvaliacaoResponse.builder()
                .documentarioId(docId)
                .valor(avaliacao.map(Avaliacao::getValor).orElse(null))
                .build();
    }

    @Transactional
    public AvaliacaoResponse avaliar(Long docId, AvaliacaoRequest request, Utilizador utilizador) {
        Documentario doc = documentarioRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Documentário não encontrado: " + docId));

        Optional<Avaliacao> existente = avaliacaoRepository.findByUtilizadorAndDocumentario(utilizador, doc);
        if (existente.isPresent()) {
            existente.get().setValor(request.getValor());
            avaliacaoRepository.save(existente.get());
        } else {
            avaliacaoRepository.save(Avaliacao.builder()
                    .utilizador(utilizador)
                    .documentario(doc)
                    .valor(request.getValor())
                    .build());
        }
        return AvaliacaoResponse.builder()
                .documentarioId(docId)
                .valor(request.getValor())
                .build();
    }

    @Transactional
    public void remover(Long docId, Utilizador utilizador) {
        Documentario doc = documentarioRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Documentário não encontrado: " + docId));
        avaliacaoRepository.deleteByUtilizadorAndDocumentario(utilizador, doc);
    }
}
