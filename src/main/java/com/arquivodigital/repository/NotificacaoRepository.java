package com.arquivodigital.repository;

import com.arquivodigital.entity.Notificacao;
import com.arquivodigital.entity.Utilizador;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByUtilizadorOrderByDataCriacaoDesc(Utilizador utilizador, Pageable pageable);

    long countByUtilizadorAndLidaFalse(Utilizador utilizador);

    @Modifying
    @Query("UPDATE Notificacao n SET n.lida = true WHERE n.utilizador = :u AND n.lida = false")
    void marcarTodasLidas(@Param("u") Utilizador utilizador);
}
