package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.vida.apirest.dto.venta.PagoVentaRequest;

@Data
public class VentaCreditoPersonalRequest {
    private Long sucursalId;
    private Long empleadoId;
    private String clienteDni;
    private String numeroFactura;
    private LocalDateTime fechaVenta;
    private String observaciones;
    private String metodoPago;
    private List<VentaDetalleRequest> detalles;
    private List<PagoVentaRequest> pagos;
    private Integer creditoPlazoMeses;
    private BigDecimal creditoTasaInteres;
    private String creditoDescripcion;
    /** Pago parcial al momento de la venta (efectivo, tarjeta, etc.) */
    private BigDecimal montoAnticipo;
    private String metodoPagoAnticipo;
    private Long cuentaIdAnticipo;
    /**
     * CUOTAS_IGUALES | ANTICIPO_SUMA_CUOTAS | PRIMERA_CUOTA_ANTICIPO | REDUCIR_PRIMERA_CUOTA
     */
    private String modoDistribucion;
    private LocalDate fechaPrimerVencimiento;
}
