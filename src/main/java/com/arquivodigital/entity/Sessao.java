package com.arquivodigital.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessoes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sessao {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @Column(nullable = false, length = 1024)
    private String token;

    @Column(nullable = false)
    private LocalDateTime dataExpiracao;

    @Column(nullable = false)
    private boolean ativa = true;

    private String ipOrigem;

    private String userAgent;

    @Column(updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    void prePersist() {
        dataCriacao = LocalDateTime.now();
    }
}
