package com.arquivodigital.service;

import com.arquivodigital.dto.request.RegistarCertificadoRequest;
import com.arquivodigital.dto.response.CertificadoResponse;
import com.arquivodigital.entity.AcaoLog;
import com.arquivodigital.entity.CertificadoDispositivo;
import com.arquivodigital.entity.EstadoCertificado;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.exception.custom.ResourceNotFoundException;
import com.arquivodigital.repository.CertificadoDispositivoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificadoService {

    private final CertificadoDispositivoRepository repository;
    private final LogService logService;

    @Transactional(readOnly = true)
    public List<CertificadoResponse> listar() {
        return repository.findAllByOrderByEmitidoEmDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CertificadoResponse registar(RegistarCertificadoRequest req, Utilizador admin, String ip) {
        CertificadoDispositivo cert = repository.findByDeviceId(req.getDeviceId())
                .orElse(CertificadoDispositivo.builder().deviceId(req.getDeviceId()).build());
        cert.setDono(req.getDono());
        cert.setSerial(req.getSerial());
        cert.setFingerprint(req.getFingerprint());
        cert.setValidadeAte(req.getValidadeAte());
        cert.setEstado(EstadoCertificado.ATIVO);
        cert.setRevogadoEm(null);
        if (cert.getEmitidoEm() == null) cert.setEmitidoEm(LocalDateTime.now());
        CertificadoDispositivo salvo = repository.save(cert);
        logService.registar(AcaoLog.CERT_REGISTADO,
                "Certificado registado para o dispositivo: " + salvo.getDeviceId(), admin, ip);
        return toResponse(salvo);
    }

    @Transactional
    public void revogar(Long id, Utilizador admin, String ip) {
        CertificadoDispositivo cert = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado não encontrado: " + id));
        cert.setEstado(EstadoCertificado.REVOGADO);
        cert.setRevogadoEm(LocalDateTime.now());
        repository.save(cert);
        logService.registar(AcaoLog.CERT_REVOGADO,
                "Certificado REVOGADO do dispositivo: " + cert.getDeviceId(), admin, ip);
    }

    /** Marca utilização do dispositivo (chamado quando um pedido mTLS chega) — rastreabilidade.
     *  Para não escrever na BD a cada pedido, só atualiza se passou mais de 1 minuto. */
    @Transactional
    public void marcarUtilizacao(String deviceId) {
        repository.findByDeviceId(deviceId).ifPresent(c -> {
            LocalDateTime agora = LocalDateTime.now();
            if (c.getUltimaUtilizacao() == null || c.getUltimaUtilizacao().isBefore(agora.minusMinutes(1))) {
                c.setUltimaUtilizacao(agora);
                repository.save(c);
            }
        });
    }

    @Transactional(readOnly = true)
    public boolean estaRevogadoPorDevice(String deviceId) {
        return repository.findByDeviceId(deviceId)
                .map(c -> c.getEstado() == EstadoCertificado.REVOGADO)
                .orElse(false);
    }

    private CertificadoResponse toResponse(CertificadoDispositivo c) {
        return CertificadoResponse.builder()
                .id(c.getId())
                .deviceId(c.getDeviceId())
                .dono(c.getDono())
                .serial(c.getSerial())
                .fingerprint(c.getFingerprint())
                .emitidoEm(c.getEmitidoEm())
                .validadeAte(c.getValidadeAte())
                .estado(c.getEstado())
                .revogadoEm(c.getRevogadoEm())
                .ultimaUtilizacao(c.getUltimaUtilizacao())
                .build();
    }
}