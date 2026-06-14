package com.vida.apirest.dto.ariticulo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class VarianteUpdateRequest {
    private Long id; 
    private String pais;           
    private String talleNumero;    
    private String color;         
    private String codigoBarras;  
    private BigDecimal precio;
    private BigDecimal costo;
    private Integer cantidad;
}
