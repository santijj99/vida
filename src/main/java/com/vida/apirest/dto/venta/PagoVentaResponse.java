package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PagoVentaResponse {
    private Long id;
    private Long cuentaId;
    private BigDecimal monto;
    private String metodoPago;
    private String numero;
    private String referencia;
    private String numeroComprobante;
    private String observaciones;
    private String estado;
    private LocalDateTime createdAt;
}
