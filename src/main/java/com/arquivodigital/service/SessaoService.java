package com.arquivodigital.service;

import com.arquivodigital.dto.response.SessaoResponse;
import com.arquivodigital.entity.Sessao;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.exception.custom.NegocioException;
import com.arquivodigital.exception.custom.ResourceNotFoundException;
import com.arquivodigital.repository.SessaoRepository;
import com.arquivodigital.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessaoService {

    private final SessaoRepository sessaoRepository;
    private final JwtUtil jwtUtil;

    @Value("${arquivo.sessao.max-por-utilizador}")
    private int maxSessoesPorUtilizador;

    @Transactional
    public Sessao criarSessao(Utilizador utilizador, String ip, String userAgent) {
        // Revogar sessões mais antigas se exceder o máximo
        long activas = sessaoRepository.countByUtilizadorAndAtivaTrue(utilizador);
        if (activas >= maxSessoesPorUtilizador) {
            sessaoRepository.revogarTodasSessoes(utilizador);
        }

        String sessaoId = UUID.randomUUID().toString();
        String token = jwtUtil.gerarToken(utilizador.getId(), utilizador.getEmail(), utilizador.getRole().name(), sessaoId);

        Sessao sessao = Sessao.builder()
                .id(sessaoId)
                .utilizador(utilizador)
                .token(token)
                .ativa(true)
                .ipOrigem(ip)
                .userAgent(userAgent)
                .dataExpiracao(LocalDateTime.now().plusSeconds(jwtUtil.getExpiracaoMs() / 1000))
                .build();

        return sessaoRepository.save(sessao);
    }

    @Transactional
    public void revogar(String sessaoId, Utilizador solicitante) {
        Sessao sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada: " + sessaoId));

        boolean ehAdmin = solicitante.getRole().name().equals("ADMIN");
        boolean ehProprietario = sessao.getUtilizador().getId().equals(solicitante.getId());

        if (!ehAdmin && !ehProprietario) {
            throw new NegocioException("Sem permissão para revogar esta sessão");
        }

        sessao.setAtiva(false);
        sessaoRepository.save(sessao);
    }

    @Transactional
    public void revogarTodasDoUtilizador(Utilizador utilizador) {
        sessaoRepository.revogarTodasSessoes(utilizador);
    }

    @Transactional(readOnly = true)
    public List<SessaoResponse> listarSessoesDo(Utilizador utilizador) {
        return sessaoRepository.findByUtilizador(utilizador).stream()
                .map(s -> SessaoResponse.builder()
                        .id(s.getId())
                        .ipOrigem(s.getIpOrigem())
                        .userAgent(s.getUserAgent())
                        .dataCriacao(s.getDataCriacao())
                        .dataExpiracao(s.getDataExpiracao())
                        .ativa(s.isAtiva())
                        .build())
                .toList();
    }

    @Transactional
    public void limparSessoesExpiradas() {
        sessaoRepository.revogarSessoesExpiradas(LocalDateTime.now());
    }
}
