package com.arquivodigital.service;

import com.arquivodigital.dto.request.LoginRequest;
import com.arquivodigital.dto.request.RegisterRequest;
import com.arquivodigital.dto.response.AuthResponse;
import com.arquivodigital.entity.AcaoLog;
import com.arquivodigital.entity.Role;
import com.arquivodigital.entity.Sessao;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.exception.custom.EmailJaExisteException;
import com.arquivodigital.mapper.UtilizadorMapper;
import com.arquivodigital.repository.UtilizadorRepository;
import com.arquivodigital.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilizadorRepository utilizadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SessaoService sessaoService;
    private final LogService logService;
    private final UtilizadorMapper utilizadorMapper;

    @Transactional
    public AuthResponse registar(RegisterRequest request, String ip) {
        if (utilizadorRepository.existsByEmail(request.getEmail())) {
            throw new EmailJaExisteException(request.getEmail());
        }

        Utilizador utilizador = Utilizador.builder()
                .nome(request.getNome())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .ativo(true)
                .build();

        utilizador = utilizadorRepository.save(utilizador);
        Sessao sessao = sessaoService.criarSessao(utilizador, ip, null);

        logService.registar(AcaoLog.REGISTO, "Novo utilizador registado: " + utilizador.getEmail(), utilizador, ip);

        return AuthResponse.builder()
                .token(sessao.getToken())
                .tipo("Bearer")
                .sessaoId(sessao.getId())
                .utilizador(utilizadorMapper.toResponse(utilizador))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ip, String userAgent) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        Utilizador utilizador = userDetails.getUtilizador();

        Sessao sessao = sessaoService.criarSessao(utilizador, ip, userAgent);

        logService.registar(AcaoLog.LOGIN, "Login a partir de " + ip, utilizador, ip);

        return AuthResponse.builder()
                .token(sessao.getToken())
                .tipo("Bearer")
                .sessaoId(sessao.getId())
                .utilizador(utilizadorMapper.toResponse(utilizador))
                .build();
    }

    @Transactional
    public void logout(String sessaoId, Utilizador utilizador, String ip) {
        sessaoService.revogar(sessaoId, utilizador);
        logService.registar(AcaoLog.LOGOUT, "Logout da sessão " + sessaoId, utilizador, ip);
    }
}
