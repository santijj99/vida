package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PagoVentaRequest {
    private Long cuentaId;
    private BigDecimal monto;
    private String metodoPago;
    private String referencia;
    private String numeroComprobante;
    private String observaciones;
    private Integer creditoPlazoMeses;
    private BigDecimal creditoTasaInteres;
    private String creditoDescripcion;
}
