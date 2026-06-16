package com.vida.apirest.dto.carrito;

import com.vida.apirest.dto.venta.VentaDetalleRequest;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ConfirmarCarritoPendienteCreditoRequest {
    private String observaciones;
    private List<VentaDetalleRequest> detalles;
    private Integer creditoPlazoMeses;
    private BigDecimal creditoTasaInteres;
    private String creditoDescripcion;
    private BigDecimal montoAnticipo;
    private String metodoPagoAnticipo;
    private Long cuentaIdAnticipo;
    private String modoDistribucion;
    private LocalDate fechaPrimerVencimiento;
}
