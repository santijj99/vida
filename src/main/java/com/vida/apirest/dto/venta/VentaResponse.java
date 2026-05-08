package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VentaResponse {
    private Long id;
    private Long clienteId;
    private String clienteDni;
    private Long empleadoId;
    private Long sucursalId;
    private String numeroFactura;
    private LocalDateTime fechaVenta;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal impuesto;
    private BigDecimal total;
    private String estado;
    private String metodoPago;
    private String observaciones;
    private List<VentaDetalleResponse> detalles;
    private List<PagoVentaResponse> pagos;
}
