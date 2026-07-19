package com.vida.apirest.dto.ariticulo;

import java.time.LocalDateTime;

public interface AuditableCatalogResponse {
    void setId(Long id);

    void setNombre(String nombre);

    void setDescripcion(String descripcion);

    void setActivo(Boolean activo);

    void setCreatedAt(LocalDateTime createdAt);

    void setUpdatedAt(LocalDateTime updatedAt);
}
