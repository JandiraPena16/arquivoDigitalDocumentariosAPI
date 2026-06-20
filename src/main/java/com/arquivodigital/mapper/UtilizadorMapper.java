package com.arquivodigital.mapper;

import com.arquivodigital.dto.response.UtilizadorResponse;
import com.arquivodigital.entity.Utilizador;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UtilizadorMapper {
    UtilizadorResponse toResponse(Utilizador utilizador);
}
