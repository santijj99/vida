package com.vida.apirest.dto.ariticulo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromocionVarianteResponse {
    private Long id;
    private Long varianteId;
    private Long articuloId;
    private String codigo;
    private String marca;
    private String modelo;
    private String talle;
    private String color;
    private BigDecimal precioOriginal;
    private BigDecimal precioPromocional;
}
