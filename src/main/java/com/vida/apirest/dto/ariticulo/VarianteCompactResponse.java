package com.vida.apirest.dto.ariticulo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VarianteCompactResponse {
    private Long id;
    private String color;
    private String talle;
    private BigDecimal precio;
    private Integer cantidad;
    private String codigoBarras;
}
