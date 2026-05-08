package com.vida.apirest.dto.ariticulo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ArticuloCreateRequest {
    private String marca;
    private String categoria;
    private String subCategoria;
    private String genero;
    private String codigo;
    private String modelo;
    private String descripcion;
    private String color;
    private List<VariantCreateRequest> variantes;
    private Long depositoId;  // ID del depósito (opcional, si es null usa el primero)
    private Long sucursalId;  // ID de la sucursal (opcional, si es null usa la primera)
}