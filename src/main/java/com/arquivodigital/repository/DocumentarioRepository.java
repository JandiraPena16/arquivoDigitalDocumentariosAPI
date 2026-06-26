package com.arquivodigital.repository;

import com.arquivodigital.entity.Documentario;
import com.arquivodigital.entity.StatusDocumentario;
import com.arquivodigital.entity.Utilizador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DocumentarioRepository extends JpaRepository<Documentario, Long> {

    Page<Documentario> findByStatus(StatusDocumentario status, Pageable pageable);

    List<Documentario> findByStatus(StatusDocumentario status);

    Page<Documentario> findByCategoriaId(Long categoriaId, Pageable pageable);

    Page<Documentario> findByCategoriaIdAndStatus(Long categoriaId, StatusDocumentario status, Pageable pageable);

    Page<Documentario> findByUtilizador(Utilizador utilizador, Pageable pageable);

    @Query("""
        SELECT d FROM Documentario d
        WHERE d.status = 'PRONTO'
        AND (:q IS NULL OR LOWER(d.titulo) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(d.descricao) LIKE LOWER(CONCAT('%', :q, '%')))
        AND (:categoriaId IS NULL OR d.categoria.id = :categoriaId)
        AND (:ano IS NULL OR d.ano = :ano)
        """)
    Page<Documentario> pesquisar(
            @Param("q") String q,
            @Param("categoriaId") Long categoriaId,
            @Param("ano") Integer ano,
            Pageable pageable
    );

    Optional<Documentario> findByIdAndStatus(Long id, StatusDocumentario status);

    @Modifying
    @Query("UPDATE Documentario d SET d.visualizacoes = d.visualizacoes + 1 WHERE d.id = :id")
    void incrementarVisualizacoes(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Documentario d SET d.downloads = d.downloads + 1 WHERE d.id = :id")
    void incrementarDownloads(@Param("id") Long id);

    List<Documentario> findTop10ByStatusOrderByVisualizacoesDesc(StatusDocumentario status);
}
