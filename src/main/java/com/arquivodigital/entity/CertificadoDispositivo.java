package com.arquivodigital.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Registo (metadados) de um certificado de cliente emitido pela CA para um dispositivo.
 * NUNCA guarda a chave privada — apenas a parte pública/identificadora,
 * para o backoffice listar dispositivos e para a rastreabilidade.
 */
@Entity
@Table(name = "certificados_dispositivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CertificadoDispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CN do certificado = identificador do dispositivo (ex.: dispositivo-001). */
    @Column(nullable = false, unique = true)
    private String deviceId;

    /** Dono/utilizador associado (nome ou email). */
    private String dono;

    /** Número de série do certificado (único por emissão). */
    @Column(nullable = false)
    private String serial;

    /** Impressão digital SHA-256 do certificado. */
    @Column(length = 200)
    private String fingerprint;

    @Column(nullable = false)
    private LocalDateTime emitidoEm;

    private LocalDateTime validadeAte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCertificado estado;

    private LocalDateTime revogadoEm;

    /** Atualizado sempre que o dispositivo faz um pedido autenticado por mTLS (rastreabilidade). */
    private LocalDateTime ultimaUtilizacao;

    @PrePersist
    void prePersist() {
        if (emitidoEm == null) emitidoEm = LocalDateTime.now();
        if (estado == null) estado = EstadoCertificado.ATIVO;
    }
}