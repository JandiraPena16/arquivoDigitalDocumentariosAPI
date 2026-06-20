package com.arquivodigital.service;

import com.arquivodigital.dto.request.AdminUpdateUserRequest;
import com.arquivodigital.dto.request.ChangePasswordRequest;
import com.arquivodigital.dto.request.UpdatePerfilRequest;
import com.arquivodigital.dto.response.PageResponse;
import com.arquivodigital.dto.response.UtilizadorResponse;
import com.arquivodigital.entity.AcaoLog;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.exception.custom.NegocioException;
import com.arquivodigital.exception.custom.ResourceNotFoundException;
import com.arquivodigital.mapper.UtilizadorMapper;
import com.arquivodigital.repository.UtilizadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UtilizadorService {

    private final UtilizadorRepository utilizadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;
    private final SessaoService sessaoService;
    private final UtilizadorMapper utilizadorMapper;

    @Transactional(readOnly = true)
    public PageResponse<UtilizadorResponse> listarTodos(int pagina, int tamanho) {
        Page<Utilizador> page = utilizadorRepository.findAll(
                PageRequest.of(pagina, tamanho, Sort.by("dataCriacao").descending())
        );
        return PageResponse.<UtilizadorResponse>builder()
                .conteudo(page.getContent().stream().map(utilizadorMapper::toResponse).toList())
                .paginaActual(page.getNumber())
                .totalPaginas(page.getTotalPages())
                .totalElementos(page.getTotalElements())
                .tamanhoPagina(page.getSize())
                .primeira(page.isFirst())
                .ultima(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public UtilizadorResponse buscarPorId(Long id) {
        return utilizadorMapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public UtilizadorResponse actualizarPerfil(Utilizador actual, UpdatePerfilRequest request) {
        actual.setNome(request.getNome());
        Utilizador salvo = utilizadorRepository.save(actual);
        logService.registar(AcaoLog.UPDATE_PERFIL, "Perfil actualizado", salvo, null);
        return utilizadorMapper.toResponse(salvo);
    }

    @Transactional
    public void alterarPassword(Utilizador utilizador, ChangePasswordRequest request, String ip) {
        if (!passwordEncoder.matches(request.getPasswordActual(), utilizador.getPassword())) {
            throw new NegocioException("A password actual está incorrecta");
        }
        utilizador.setPassword(passwordEncoder.encode(request.getNovaPassword()));
        utilizadorRepository.save(utilizador);
        sessaoService.revogarTodasDoUtilizador(utilizador);
        logService.registar(AcaoLog.CHANGE_PASSWORD, "Password alterada — todas as sessões revogadas", utilizador, ip);
    }

    @Transactional
    public UtilizadorResponse adminActualizar(Long id, AdminUpdateUserRequest request, Utilizador admin, String ip) {
        Utilizador utilizador = buscarEntidade(id);
        if (request.getNome() != null) utilizador.setNome(request.getNome());
        if (request.getRole() != null) utilizador.setRole(request.getRole());
        if (request.getAtivo() != null) utilizador.setAtivo(request.getAtivo());
        Utilizador salvo = utilizadorRepository.save(utilizador);
        logService.registar(AcaoLog.ADMIN_UPDATE_USER, "Admin actualizou utilizador ID " + id, admin, ip);
        return utilizadorMapper.toResponse(salvo);
    }

    @Transactional
    public void eliminar(Long id, Utilizador admin, String ip) {
        Utilizador utilizador = buscarEntidade(id);
        if (utilizador.getId().equals(admin.getId())) {
            throw new NegocioException("Não pode eliminar a sua própria conta");
        }
        sessaoService.revogarTodasDoUtilizador(utilizador);
        utilizadorRepository.delete(utilizador);
        logService.registar(AcaoLog.ADMIN_DELETE_USER, "Admin eliminou utilizador: " + utilizador.getEmail(), admin, ip);
    }

    public Utilizador buscarEntidade(Long id) {
        return utilizadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado: " + id));
    }
}
