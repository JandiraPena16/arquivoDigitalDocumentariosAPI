package com.arquivodigital.service;

import com.arquivodigital.dto.response.EnrollmentResponse;
import com.arquivodigital.entity.AcaoLog;
import com.arquivodigital.entity.CertificadoDispositivo;
import com.arquivodigital.entity.EstadoCertificado;
import com.arquivodigital.entity.Utilizador;
import com.arquivodigital.exception.custom.NegocioException;
import com.arquivodigital.repository.CertificadoDispositivoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Enrolamento automático: gera (via OpenSSL) um certificado de cliente próprio para
 * um dispositivo, assinado pela CA, regista-o na BD e devolve o PKCS#12 à app.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final CertificadoDispositivoRepository certRepo;
    private final LogService logService;

    @Value("${arquivo.pki.dir:./pki}")
    private String pkiDir;

    @Value("${arquivo.pki.openssl-path:openssl}")
    private String openssl;

    @Transactional
    public EnrollmentResponse enrolar(String deviceIdRaw, String deviceName, Utilizador dono, String ip) {
        String deviceId = sanitizar(deviceIdRaw);
        File base = new File(pkiDir);
        File caCrt = new File(base, "ca/ca.crt");
        File caKey = new File(base, "ca/ca.key");
        if (!caCrt.exists() || !caKey.exists()) {
            throw new NegocioException("A CA não está disponível no servidor (pki/ca/). Corre o gerar_pki.sh.");
        }

        File dir = new File(base, "devices/" + deviceId);
        dir.mkdirs();
        File key = new File(dir, deviceId + ".key");
        File csr = new File(dir, deviceId + ".csr");
        File crt = new File(dir, deviceId + ".crt");
        File ext = new File(dir, "ext.cnf");
        File p12 = new File(dir, deviceId + ".p12");
        String pass = gerarPassword();

        try {
            Files.writeString(ext.toPath(),
                    "basicConstraints=CA:FALSE\nkeyUsage=digitalSignature\nextendedKeyUsage=clientAuth\n");

            run(base, openssl, "genrsa", "-out", key.getAbsolutePath(), "2048");
            run(base, openssl, "req", "-new", "-key", key.getAbsolutePath(), "-out", csr.getAbsolutePath(),
                    "-subj", "/C=AO/O=Arquivo Digital de Documentarios/OU=Dispositivos/CN=" + deviceId);
            run(base, openssl, "x509", "-req", "-in", csr.getAbsolutePath(),
                    "-CA", caCrt.getAbsolutePath(), "-CAkey", caKey.getAbsolutePath(), "-CAcreateserial",
                    "-out", crt.getAbsolutePath(), "-days", "825", "-sha256", "-extfile", ext.getAbsolutePath());
            run(base, openssl, "pkcs12", "-export", "-in", crt.getAbsolutePath(), "-inkey", key.getAbsolutePath(),
                    "-certfile", caCrt.getAbsolutePath(), "-name", deviceId, "-out", p12.getAbsolutePath(),
                    "-password", "pass:" + pass);

            String serial = capturar(base, openssl, "x509", "-in", crt.getAbsolutePath(), "-noout", "-serial")
                    .replace("serial=", "").trim();
            String fpr = capturar(base, openssl, "x509", "-in", crt.getAbsolutePath(), "-noout",
                    "-fingerprint", "-sha256");
            fpr = fpr.contains("=") ? fpr.substring(fpr.indexOf('=') + 1).trim() : fpr.trim();
            LocalDateTime validade = parseValidade(
                    capturar(base, openssl, "x509", "-in", crt.getAbsolutePath(), "-noout", "-enddate"));

            byte[] p12bytes = Files.readAllBytes(p12.toPath());

            CertificadoDispositivo cert = certRepo.findByDeviceId(deviceId)
                    .orElse(CertificadoDispositivo.builder().deviceId(deviceId).build());
            cert.setDono(dono != null ? dono.getEmail() : deviceName);
            cert.setSerial(serial);
            cert.setFingerprint(fpr);
            cert.setValidadeAte(validade);
            cert.setEstado(EstadoCertificado.ATIVO);
            cert.setRevogadoEm(null);
            if (cert.getEmitidoEm() == null) cert.setEmitidoEm(LocalDateTime.now());
            certRepo.save(cert);

            logService.registar(AcaoLog.CERT_REGISTADO,
                    "Enrolamento automático do dispositivo: " + deviceId
                            + (deviceName != null ? " (" + deviceName + ")" : ""), dono, ip);

            return EnrollmentResponse.builder()
                    .deviceId(deviceId)
                    .password(pass)
                    .p12Base64(Base64.getEncoder().encodeToString(p12bytes))
                    .build();

        } catch (NegocioException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha no enrolamento do dispositivo {}", deviceId, e);
            throw new NegocioException("Falha no enrolamento: " + e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String sanitizar(String id) {
        String s = id == null ? "" : id.replaceAll("[^a-zA-Z0-9_-]", "").trim();
        if (s.isEmpty()) throw new NegocioException("deviceId inválido");
        return s.length() > 60 ? s.substring(0, 60) : s;
    }

    private String gerarPassword() {
        byte[] b = new byte[18];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private void run(File dir, String... cmd) throws Exception {
        if (capturarComExit(dir, cmd).exit != 0) {
            throw new NegocioException("Comando OpenSSL falhou: " + String.join(" ", cmd));
        }
    }

    private String capturar(File dir, String... cmd) throws Exception {
        Resultado r = capturarComExit(dir, cmd);
        if (r.exit != 0) throw new NegocioException("Comando OpenSSL falhou: " + String.join(" ", cmd));
        return r.saida;
    }

    private Resultado capturarComExit(File dir, String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).directory(dir).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(30, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new NegocioException("OpenSSL demorou demasiado"); }
        return new Resultado(p.exitValue(), out);
    }

    private LocalDateTime parseValidade(String enddate) {
        try {
            String s = enddate.replace("notAfter=", "").trim().replaceAll("\\s+", " ");
            // ex.: "Sep 27 15:19:44 2028 GMT"
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d HH:mm:ss yyyy zzz", Locale.ENGLISH);
            return java.time.ZonedDateTime.parse(s, fmt).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now().plusDays(825);
        }
    }

    private record Resultado(int exit, String saida) {}
}