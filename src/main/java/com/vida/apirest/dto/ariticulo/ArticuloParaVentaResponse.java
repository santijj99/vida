package com.vida.apirest.dto.ariticulo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloParaVentaResponse {
    private Long articuloId;
    private Long varianteId;
    private String codigo;
    private String marca;
    private String modelo;
    private String talle;
    private String color;
    private String codigoBarras;
    private BigDecimal precio;
    private Integer stockDisponible;
}
