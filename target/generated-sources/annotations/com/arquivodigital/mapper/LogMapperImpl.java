package com.arquivodigital.mapper;

import com.arquivodigital.dto.response.LogResponse;
import com.arquivodigital.entity.Log;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-20T11:23:05+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class LogMapperImpl implements LogMapper {

    @Override
    public LogResponse toResponse(Log log) {
        if ( log == null ) {
            return null;
        }

        LogResponse.LogResponseBuilder logResponse = LogResponse.builder();

        logResponse.acao( log.getAcao() );
        logResponse.detalhe( log.getDetalhe() );
        logResponse.id( log.getId() );
        logResponse.ip( log.getIp() );
        logResponse.timestamp( log.getTimestamp() );

        logResponse.nomeUtilizador( log.getUtilizador() != null ? log.getUtilizador().getNome() : "Sistema" );
        logResponse.utilizadorId( log.getUtilizador() != null ? log.getUtilizador().getId() : null );

        return logResponse.build();
    }
}
