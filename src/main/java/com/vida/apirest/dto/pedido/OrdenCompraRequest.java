package com.vida.apirest.dto.pedido;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrdenCompraRequest {
    private Long sucursalId;
    private Long proveedorId;
    private Long depositoId;
    private LocalDateTime fechaEntregaEstimada;
    private BigDecimal descuento;
    private BigDecimal impuesto;
    private String condicionPago;
    private String observaciones;
    private List<OrdenCompraDetalleRequest> detalles;
}
