package com.arquivodigital.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "minha_lista", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"utilizador_id", "documentario_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MinhaLista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documentario_id", nullable = false)
    private Documentario documentario;

    @Column(updatable = false)
    private LocalDateTime dataAdicionado;

    @PrePersist
    void prePersist() {
        dataAdicionado = LocalDateTime.now();
    }
}
