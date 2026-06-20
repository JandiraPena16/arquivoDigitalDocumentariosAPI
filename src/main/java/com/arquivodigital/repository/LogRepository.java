package com.arquivodigital.repository;

import com.arquivodigital.entity.AcaoLog;
import com.arquivodigital.entity.Log;
import com.arquivodigital.entity.Utilizador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogRepository extends JpaRepository<Log, Long> {

    Page<Log> findByUtilizador(Utilizador utilizador, Pageable pageable);

    Page<Log> findByUtilizadorId(Long utilizadorId, Pageable pageable);

    @Query("""
        SELECT l FROM Log l
        WHERE (:acao IS NULL OR l.acao = :acao)
        AND (:utilizadorId IS NULL OR l.utilizador.id = :utilizadorId)
        ORDER BY l.timestamp DESC
        """)
    Page<Log> filtrar(
            @Param("acao") AcaoLog acao,
            @Param("utilizadorId") Long utilizadorId,
            Pageable pageable
    );
}
