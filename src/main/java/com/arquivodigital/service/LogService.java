package com.arquivodigital.service;

import com.arquivodigital.dto.response.LogResponse;
import com.arquivodigital.dto.response.PageResponse;
import com.arquivodigital.entity.AcaoLog;
import com.arquivodigital.entity.Log;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.mapper.LogMapper;
import com.arquivodigital.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;
    private final LogMapper logMapper;

    @Transactional
    public void registar(AcaoLog acao, String detalhe, Utilizador utilizador, String ip) {
        Log log = Log.builder()
                .acao(acao)
                .detalhe(detalhe)
                .utilizador(utilizador)
                .ip(ip)
                .build();
        logRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PageResponse<LogResponse> listarTodos(AcaoLog acao, Long utilizadorId, int pagina, int tamanho) {
        Page<Log> page = logRepository.filtrar(
                acao, utilizadorId,
                PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "timestamp"))
        );
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<LogResponse> listarPorUtilizador(Utilizador utilizador, int pagina, int tamanho) {
        Page<Log> page = logRepository.findByUtilizador(
                utilizador,
                PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "timestamp"))
        );
        return toPageResponse(page);
    }

    @Transactional
    public void limparTodos() {
        logRepository.deleteAll();
    }

    private PageResponse<LogResponse> toPageResponse(Page<Log> page) {
        return PageResponse.<LogResponse>builder()
                .conteudo(page.getContent().stream().map(logMapper::toResponse).toList())
                .paginaActual(page.getNumber())
                .totalPaginas(page.getTotalPages())
                .totalElementos(page.getTotalElements())
                .tamanhoPagina(page.getSize())
                .primeira(page.isFirst())
                .ultima(page.isLast())
                .build();
    }
}
