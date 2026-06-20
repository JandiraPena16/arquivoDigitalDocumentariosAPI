package com.arquivodigital.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Documentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    private Integer ano;

    private Long duracaoSegundos;

    private String formato;

    // Caminhos em disco
    @Column(nullable = false)
    private String caminhoOriginal;

    private String caminhoComprimido;

    private String caminhoThumbnail;

    // Estatísticas de compressão
    private Long tamanhoOriginalBytes;
    private Long tamanhoComprimidoBytes;
    private Double taxaCompressao;
    private Long tempoProcessamentoMs;
    private String codecVideo;
    private String codecAudio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDocumentario status;

    // Métricas
    @Builder.Default
    private Long visualizacoes = 0L;

    @Builder.Default
    private Long downloads = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(updatable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataAtualizacao;

    @PrePersist
    void prePersist() {
        dataCriacao = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
        if (status == null) status = StatusDocumentario.PENDENTE;
        if (visualizacoes == null) visualizacoes = 0L;
        if (downloads == null) downloads = 0L;
    }

    @PreUpdate
    void preUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}
