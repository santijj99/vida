package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CerrarCajaRequest {
    private BigDecimal montoContado;
    private String observaciones;
}
