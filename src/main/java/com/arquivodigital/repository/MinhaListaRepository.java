package com.arquivodigital.repository;

import com.arquivodigital.entity.Documentario;
import com.arquivodigital.entity.MinhaLista;
import com.arquivodigital.entity.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MinhaListaRepository extends JpaRepository<MinhaLista, Long> {

    List<MinhaLista> findByUtilizadorOrderByDataAdicionadoDesc(Utilizador utilizador);

    boolean existsByUtilizadorAndDocumentario(Utilizador utilizador, Documentario documentario);

    @Modifying
    @Query("DELETE FROM MinhaLista m WHERE m.utilizador = :u AND m.documentario = :d")
    void deleteByUtilizadorAndDocumentario(@Param("u") Utilizador utilizador, @Param("d") Documentario documentario);

    boolean existsByUtilizadorIdAndDocumentarioId(Long utilizadorId, Long documentarioId);
}
