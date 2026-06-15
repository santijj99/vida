package com.vida.apirest.dto.ariticulo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromocionVarianteRequest {
    private Long varianteId;
    private BigDecimal precioPromocional;
}
