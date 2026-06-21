package com.arquivodigital.repository;

import com.arquivodigital.entity.Documentario;
import com.arquivodigital.entity.HistoricoItem;
import com.arquivodigital.entity.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface HistoricoRepository extends JpaRepository<HistoricoItem, Long> {

    List<HistoricoItem> findByUtilizadorOrderByDataVistoDesc(Utilizador utilizador);

    Optional<HistoricoItem> findByUtilizadorAndDocumentario(Utilizador utilizador, Documentario documentario);

    @Modifying
    @Query("DELETE FROM HistoricoItem h WHERE h.utilizador = :u")
    void deleteAllByUtilizador(@Param("u") Utilizador utilizador);
}
