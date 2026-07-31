package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private BigDecimal creditoMontoAnticipo;
    private String creditoModoDistribucion;
    /** Fecha del 1er vencimiento elegida en el popup de crédito. */
    private LocalDate fechaPrimerVencimiento;
}
