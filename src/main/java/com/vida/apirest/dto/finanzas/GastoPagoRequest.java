package com.vida.apirest.dto.finanzas;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GastoPagoRequest {
    private Long cuentaId;
    private BigDecimal monto;
    private String numeroComprobante;
    private String referencia;
    private String observaciones;
}
