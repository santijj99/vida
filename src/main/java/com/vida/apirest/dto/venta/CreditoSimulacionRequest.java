package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreditoSimulacionRequest {
    private BigDecimal montoTotal;
    private List<VentaDetalleRequest> detalles;
    private Integer plazoMeses;
    private BigDecimal tasaInteres;
    private BigDecimal montoAnticipo;
    /**
     * CUOTAS_IGUALES | ANTICIPO_SUMA_CUOTAS | PRIMERA_CUOTA_ANTICIPO | REDUCIR_PRIMERA_CUOTA
     */
    private String modoDistribucion;
    /** Fecha de vencimiento de la primera cuota (las demás +1 mes cada una). */
    private LocalDate fechaPrimerVencimiento;
}
