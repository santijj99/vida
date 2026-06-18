package com.vida.apirest.dto.finanzas;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GastoPagoResponse {
    private Long id;
    private Long cuentaId;
    private String cuentaNombre;
    private String cuentaNumero;
    private String cuentaTipo;
    private BigDecimal monto;
    private String numeroComprobante;
    private String referencia;
    private String observaciones;
    private LocalDateTime createdAt;
}
