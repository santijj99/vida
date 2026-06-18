package com.vida.apirest.dto.finanzas;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GastoUpdateRequest {
    private Long categoriaId;
    private String descripcion;
    private BigDecimal monto;
    private Long monedaId;
    private String proveedor;
    private String numeroComprobante;
    private LocalDateTime fechaComprobante;
    private String responsable;
    private String observaciones;
}
