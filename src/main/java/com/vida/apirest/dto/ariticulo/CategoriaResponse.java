package com.vida.apirest.dto.ariticulo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoriaResponse implements AuditableCatalogResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
