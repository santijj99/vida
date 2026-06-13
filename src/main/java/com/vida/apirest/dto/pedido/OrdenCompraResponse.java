package com.vida.apirest.dto.pedido;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrdenCompraResponse {
    private Long id;
    private String numero;
    private Long sucursalId;
    private String sucursalNombre;
    private Long proveedorId;
    private String proveedorNombre;
    private String proveedorCodigo;
    private LocalDateTime fechaOrden;
    private LocalDateTime fechaEntregaEstimada;
    private LocalDateTime fechaEntregaReal;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal impuesto;
    private BigDecimal total;
    private String estado;
    private String condicionPago;
    private String observaciones;
    private String responsable;
    private List<OrdenCompraDetalleResponse> detalles;
}
