package com.vida.apirest.utils;

import com.vida.apirest.dto.ariticulo.AuditableCatalogResponse;
import com.vida.apirest.model.articulo.Categoria;
import com.vida.apirest.model.articulo.Genero;
import com.vida.apirest.model.articulo.Taxon;

public final class CatalogMapper {

    private CatalogMapper() {
    }

    public static <R extends AuditableCatalogResponse> R mapAuditable(R response, Categoria entity) {
        fillAuditable(response, entity.getId(), entity.getNombre(), entity.getDescripcion(),
                entity.getActivo(), entity.getCreatedAt(), entity.getUpdatedAt());
        return response;
    }

    public static <R extends AuditableCatalogResponse> R mapAuditable(R response, Genero entity) {
        fillAuditable(response, entity.getId(), entity.getNombre(), entity.getDescripcion(),
                entity.getActivo(), entity.getCreatedAt(), entity.getUpdatedAt());
        return response;
    }

    public static <R extends AuditableCatalogResponse> R mapAuditable(R response, Taxon entity) {
        fillAuditable(response, entity.getId(), entity.getNombre(), entity.getDescripcion(),
                entity.getActivo(), entity.getCreatedAt(), entity.getUpdatedAt());
        return response;
    }

    private static void fillAuditable(
            AuditableCatalogResponse response,
            Long id,
            String nombre,
            String descripcion,
            Boolean activo,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {
        response.setId(id);
        response.setNombre(nombre);
        response.setDescripcion(descripcion);
        response.setActivo(activo);
        response.setCreatedAt(createdAt);
        response.setUpdatedAt(updatedAt);
    }
}
