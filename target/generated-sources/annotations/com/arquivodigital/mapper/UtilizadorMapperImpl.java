package com.arquivodigital.mapper;

import com.arquivodigital.dto.response.UtilizadorResponse;
import com.arquivodigital.entity.Utilizador;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-20T11:23:05+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class UtilizadorMapperImpl implements UtilizadorMapper {

    @Override
    public UtilizadorResponse toResponse(Utilizador utilizador) {
        if ( utilizador == null ) {
            return null;
        }

        UtilizadorResponse.UtilizadorResponseBuilder utilizadorResponse = UtilizadorResponse.builder();

        utilizadorResponse.ativo( utilizador.isAtivo() );
        utilizadorResponse.dataCriacao( utilizador.getDataCriacao() );
        utilizadorResponse.email( utilizador.getEmail() );
        utilizadorResponse.id( utilizador.getId() );
        utilizadorResponse.nome( utilizador.getNome() );
        utilizadorResponse.role( utilizador.getRole() );

        return utilizadorResponse.build();
    }
}
