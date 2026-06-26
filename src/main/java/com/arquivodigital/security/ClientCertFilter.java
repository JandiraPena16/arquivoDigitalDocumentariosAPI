package com.arquivodigital.security;

import com.arquivodigital.service.CertificadoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fase 4 — Rastreabilidade por dispositivo (mTLS).
 *
 * Lê o certificado de cliente apresentado na ligação TLS (preenchido pelo Tomcat quando o
 * client-auth está ativo), extrai o CN (= id do dispositivo) e:
 *   - bloqueia o pedido se o certificado estiver REVOGADO;
 *   - regista a última utilização do dispositivo;
 *   - coloca o id no {@link DispositivoContext} para os logs ficarem com "quem fez o quê".
 *
 * Enquanto o mTLS não estiver ligado (Fases 5/6), não há certificado e o filtro não faz nada
 * (a app continua a funcionar normalmente).
 */
@Component
@RequiredArgsConstructor
public class ClientCertFilter extends OncePerRequestFilter {

    private static final Pattern CN = Pattern.compile("CN=([^,]+)");
    private final CertificadoService certificadoService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            X509Certificate[] certs = (X509Certificate[])
                    request.getAttribute("jakarta.servlet.request.X509Certificate");
            if (certs != null && certs.length > 0) {
                String deviceId = extrairCn(certs[0].getSubjectX500Principal().getName());
                if (deviceId != null) {
                    if (certificadoService.estaRevogadoPorDevice(deviceId)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                "Certificado do dispositivo revogado");
                        return;
                    }
                    DispositivoContext.set(deviceId);
                    certificadoService.marcarUtilizacao(deviceId);
                }
            }
            chain.doFilter(request, response);
        } finally {
            DispositivoContext.clear();
        }
    }

    private String extrairCn(String dn) {
        if (dn == null) return null;
        Matcher m = CN.matcher(dn);
        return m.find() ? m.group(1).trim() : null;
    }
}