package com.vida.apirest.dto.ariticulo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VariantCreateRequest {
    private String pais;           // AR, UK, BR, US, EU
    private String talleNumero;    // 30, 35, 36, 42
    private String color;         // color por variante
    private String codigoBarras;  // opcional, único
    private BigDecimal precio;
    private BigDecimal costo;
    private Integer cantidad;
}