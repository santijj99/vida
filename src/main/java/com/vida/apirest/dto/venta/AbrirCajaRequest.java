package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AbrirCajaRequest {
    private Long cuentaId;
    private BigDecimal montoApertura;
}
