package com.arquivodigital.repository;

import com.arquivodigital.entity.Sessao;
import com.arquivodigital.entity.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessaoRepository extends JpaRepository<Sessao, String> {

    Optional<Sessao> findByIdAndAtivaTrue(String id);

    List<Sessao> findByUtilizadorAndAtivaTrue(Utilizador utilizador);

    List<Sessao> findByUtilizador(Utilizador utilizador);

    long countByUtilizadorAndAtivaTrue(Utilizador utilizador);

    @Modifying
    @Query("UPDATE Sessao s SET s.ativa = false WHERE s.utilizador = :utilizador AND s.ativa = true")
    void revogarTodasSessoes(Utilizador utilizador);

    @Modifying
    @Query("UPDATE Sessao s SET s.ativa = false WHERE s.dataExpiracao < :agora AND s.ativa = true")
    void revogarSessoesExpiradas(LocalDateTime agora);
}
