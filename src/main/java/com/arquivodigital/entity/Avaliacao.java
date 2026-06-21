package com.arquivodigital.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "avaliacoes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"utilizador_id", "documentario_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documentario_id", nullable = false)
    private Documentario documentario;

    /** 1 = gosto, -1 = não gosto */
    @Column(nullable = false)
    private Integer valor;

    private LocalDateTime dataAvaliacao;

    @PrePersist
    void prePersist() {
        dataAvaliacao = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        dataAvaliacao = LocalDateTime.now();
    }
}
