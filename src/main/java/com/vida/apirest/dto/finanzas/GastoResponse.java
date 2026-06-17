package com.vida.apirest.dto.finanzas;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GastoResponse {
    private Long id;
    private String numero;
    private Long sucursalId;
    private String sucursalNombre;
    private Long categoriaId;
    private String categoriaNombre;
    private String descripcion;
    private BigDecimal monto;
    private Long monedaId;
    private String estado;
    private String proveedor;
    private String numeroComprobante;
    private LocalDateTime fechaComprobante;
    private String responsable;
    private String observaciones;
    private BigDecimal totalPagado;
    private BigDecimal saldoPendiente;
    private List<GastoPagoResponse> pagos;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
