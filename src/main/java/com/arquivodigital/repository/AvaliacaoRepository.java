package com.arquivodigital.repository;

import com.arquivodigital.entity.Avaliacao;
import com.arquivodigital.entity.Documentario;
import com.arquivodigital.entity.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    Optional<Avaliacao> findByUtilizadorAndDocumentario(Utilizador utilizador, Documentario documentario);

    @Modifying
    @Query("DELETE FROM Avaliacao a WHERE a.utilizador = :u AND a.documentario = :d")
    void deleteByUtilizadorAndDocumentario(@Param("u") Utilizador utilizador, @Param("d") Documentario documentario);

    @Query("SELECT COUNT(a) FROM Avaliacao a WHERE a.documentario.id = :docId AND a.valor = 1")
    long countLikesByDocumentarioId(@Param("docId") Long docId);

    @Query("SELECT COUNT(a) FROM Avaliacao a WHERE a.documentario.id = :docId AND a.valor = -1")
    long countDislikesByDocumentarioId(@Param("docId") Long docId);

    /** Utilizadores que deram like a pelo menos um vídeo de uma categoria. */
    @Query("SELECT DISTINCT a.utilizador FROM Avaliacao a WHERE a.valor = 1 AND a.documentario.categoria.id = :catId")
    List<Utilizador> findUtilizadoresQueGostaramDaCategoria(@Param("catId") Long catId);

    /** Todas as avaliações de um utilizador (mais recentes primeiro). */
    List<Avaliacao> findByUtilizadorOrderByDataAvaliacaoDesc(Utilizador utilizador);
}
