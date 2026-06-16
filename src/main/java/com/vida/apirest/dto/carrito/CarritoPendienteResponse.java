package com.vida.apirest.dto.carrito;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CarritoPendienteResponse {
    private Long id;
    private String numeroComprobante;
    private Long sucursalId;
    private String sucursalNombre;
    private Long clienteId;
    private String clienteDni;
    private String clienteNombre;
    private String empleadoNombre;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaCierre;
    private String observaciones;
    private Long ventaId;
    private String numeroFactura;
    private BigDecimal total;
    private List<CarritoPendienteDetalleResponse> detalles;
}
