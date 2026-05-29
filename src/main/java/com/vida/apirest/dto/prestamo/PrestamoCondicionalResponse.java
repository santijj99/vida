package com.vida.apirest.dto.prestamo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PrestamoCondicionalResponse {
    private Long id;
    private String numeroComprobante;
    private Long sucursalId;
    private String sucursalNombre;
    private Long clienteId;
    private String clienteDni;
    private String clienteNombre;
    private String estado;
    private LocalDateTime fechaEntrega;
    private LocalDate fechaLimite;
    private LocalDateTime fechaCierre;
    private String observaciones;
    private Long ventaId;
    private String numeroFactura;
    private BigDecimal totalReferencia;
    private BigDecimal totalPendiente;
    private List<PrestamoCondicionalDetalleResponse> detalles;
}
