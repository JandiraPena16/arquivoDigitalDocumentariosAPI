package com.arquivodigital.mapper;

import com.arquivodigital.dto.request.CategoriaRequest;
import com.arquivodigital.dto.response.CategoriaResponse;
import com.arquivodigital.entity.Categoria;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {
    CategoriaResponse toResponse(Categoria categoria);
    Categoria toEntity(CategoriaRequest request);
    void updateEntity(CategoriaRequest request, @MappingTarget Categoria categoria);
}
