package com.arquivodigital.repository;

import com.arquivodigital.entity.Avaliacao;
import com.arquivodigital.entity.Documentario;
import com.arquivodigital.entity.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    Optional<Avaliacao> findByUtilizadorAndDocumentario(Utilizador utilizador, Documentario documentario);

    @Modifying
    @Query("DELETE FROM Avaliacao a WHERE a.utilizador = :u AND a.documentario = :d")
    void deleteByUtilizadorAndDocumentario(@Param("u") Utilizador utilizador, @Param("d") Documentario documentario);
}
