package com.arquivodigital.service;

import com.arquivodigital.dto.response.DocumentarioResponse;
import com.arquivodigital.entity.Documentario;
import com.arquivodigital.entity.MinhaLista;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.exception.custom.ResourceNotFoundException;
import com.arquivodigital.repository.DocumentarioRepository;
import com.arquivodigital.repository.MinhaListaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MinhaListaService {

    private final MinhaListaRepository minhaListaRepository;
    private final DocumentarioRepository documentarioRepository;
    private final DocumentarioService documentarioService;

    @Transactional(readOnly = true)
    public List<DocumentarioResponse> listar(Utilizador utilizador) {
        return minhaListaRepository.findByUtilizadorOrderByDataAdicionadoDesc(utilizador)
                .stream()
                .map(item -> documentarioService.toResponse(item.getDocumentario()))
                .toList();
    }

    @Transactional
    public void adicionar(Long docId, Utilizador utilizador) {
        Documentario doc = documentarioRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Documentário não encontrado: " + docId));

        if (!minhaListaRepository.existsByUtilizadorAndDocumentario(utilizador, doc)) {
            minhaListaRepository.save(MinhaLista.builder()
                    .utilizador(utilizador)
                    .documentario(doc)
                    .build());
        }
    }

    @Transactional
    public void remover(Long docId, Utilizador utilizador) {
        Documentario doc = documentarioRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Documentário não encontrado: " + docId));
        minhaListaRepository.deleteByUtilizadorAndDocumentario(utilizador, doc);
    }

    @Transactional(readOnly = true)
    public boolean verificar(Long docId, Utilizador utilizador) {
        return minhaListaRepository.existsByUtilizadorIdAndDocumentarioId(utilizador.getId(), docId);
    }
}
