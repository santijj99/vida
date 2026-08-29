package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AjusteCajaRequest {
    private Long cuentaId;
    /** FALTANTE | SOBRANTE */
    private String sentido;
    private BigDecimal monto;
    private String motivo;
}
