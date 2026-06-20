package com.arquivodigital.mapper;

import com.arquivodigital.dto.request.CategoriaRequest;
import com.arquivodigital.dto.response.CategoriaResponse;
import com.arquivodigital.entity.Categoria;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-20T11:23:04+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class CategoriaMapperImpl implements CategoriaMapper {

    @Override
    public CategoriaResponse toResponse(Categoria categoria) {
        if ( categoria == null ) {
            return null;
        }

        CategoriaResponse.CategoriaResponseBuilder categoriaResponse = CategoriaResponse.builder();

        categoriaResponse.descricao( categoria.getDescricao() );
        categoriaResponse.id( categoria.getId() );
        categoriaResponse.nome( categoria.getNome() );

        return categoriaResponse.build();
    }

    @Override
    public Categoria toEntity(CategoriaRequest request) {
        if ( request == null ) {
            return null;
        }

        Categoria.CategoriaBuilder categoria = Categoria.builder();

        categoria.descricao( request.getDescricao() );
        categoria.nome( request.getNome() );

        return categoria.build();
    }

    @Override
    public void updateEntity(CategoriaRequest request, Categoria categoria) {
        if ( request == null ) {
            return;
        }

        categoria.setDescricao( request.getDescricao() );
        categoria.setNome( request.getNome() );
    }
}
