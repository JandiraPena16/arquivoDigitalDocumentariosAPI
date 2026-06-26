package com.arquivodigital.repository;

import com.arquivodigital.entity.CertificadoDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificadoDispositivoRepository extends JpaRepository<CertificadoDispositivo, Long> {

    Optional<CertificadoDispositivo> findByDeviceId(String deviceId);

    Optional<CertificadoDispositivo> findBySerial(String serial);

    List<CertificadoDispositivo> findAllByOrderByEmitidoEmDesc();

    boolean existsByDeviceId(String deviceId);
}