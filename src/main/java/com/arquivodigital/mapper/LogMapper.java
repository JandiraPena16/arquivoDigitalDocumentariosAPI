package com.arquivodigital.mapper;

import com.arquivodigital.dto.response.LogResponse;
import com.arquivodigital.entity.Log;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LogMapper {

    @Mapping(target = "nomeUtilizador", expression = "java(log.getUtilizador() != null ? log.getUtilizador().getNome() : \"Sistema\")")
    @Mapping(target = "utilizadorId", expression = "java(log.getUtilizador() != null ? log.getUtilizador().getId() : null)")
    LogResponse toResponse(Log log);
}
