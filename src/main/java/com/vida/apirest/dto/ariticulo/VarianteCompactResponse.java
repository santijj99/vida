package com.vida.apirest.dto.ariticulo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VarianteCompactResponse {
    private Long id;
    private String color;
    private String talle;
    /** Escala del talle: AR, UK, BR, US, EU */
    private String pais;
    private BigDecimal precio;
    private Integer cantidad;
    private String codigoBarras;
}
