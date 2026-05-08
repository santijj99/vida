package com.vida.apirest.dto.finanzas;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateMonedaRequest {
    private String codigo;
    private String nombre;
    private String simbolo;
    private BigDecimal tasaCambio;
    private Integer decimalPlaces;
    private Boolean activo;
    private Boolean predeterminada;
}
