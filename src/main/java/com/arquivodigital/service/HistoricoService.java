package com.arquivodigital.service;

import com.arquivodigital.dto.response.DocumentarioResponse;
import com.arquivodigital.entity.Documentario;
import com.arquivodigital.entity.HistoricoItem;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.exception.custom.ResourceNotFoundException;
import com.arquivodigital.repository.DocumentarioRepository;
import com.arquivodigital.repository.HistoricoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HistoricoService {

    private final HistoricoRepository historicoRepository;
    private final DocumentarioRepository documentarioRepository;
    private final DocumentarioService documentarioService;

    @Transactional(readOnly = true)
    public List<DocumentarioResponse> listar(Utilizador utilizador) {
        return historicoRepository.findByUtilizadorOrderByDataVistoDesc(utilizador)
                .stream()
                .map(item -> documentarioService.toResponse(item.getDocumentario()))
                .toList();
    }

    @Transactional
    public void registar(Long docId, Utilizador utilizador) {
        Documentario doc = documentarioRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Documentário não encontrado: " + docId));

        Optional<HistoricoItem> existente = historicoRepository.findByUtilizadorAndDocumentario(utilizador, doc);
        if (existente.isPresent()) {
            // Utilizador já assistiu — apenas atualiza o timestamp, não conta nova visualização
            existente.get().setDataVisto(LocalDateTime.now());
            historicoRepository.save(existente.get());
        } else {
            // Primeira vez que este utilizador assiste — cria entrada E conta visualização
            historicoRepository.save(HistoricoItem.builder()
                    .utilizador(utilizador)
                    .documentario(doc)
                    .dataVisto(LocalDateTime.now())
                    .build());
            documentarioRepository.incrementarVisualizacoes(docId);
        }
    }

    @Transactional
    public void limpar(Utilizador utilizador) {
        historicoRepository.deleteAllByUtilizador(utilizador);
    }
}
