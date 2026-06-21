package com.arquivodigital.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"utilizador_id", "documentario_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistoricoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documentario_id", nullable = false)
    private Documentario documentario;

    private LocalDateTime dataVisto;

    @PrePersist
    void prePersist() {
        dataVisto = LocalDateTime.now();
    }
}
