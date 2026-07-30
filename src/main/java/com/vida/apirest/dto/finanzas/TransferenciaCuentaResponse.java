package com.vida.apirest.dto.finanzas;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransferenciaCuentaResponse {
    private Long movimientoOrigenId;
    private Long movimientoDestinoId;
    private Long cuentaOrigenId;
    private String cuentaOrigenNombre;
    private Long cuentaDestinoId;
    private String cuentaDestinoNombre;
    private BigDecimal monto;
    private BigDecimal saldoOrigenNuevo;
    private BigDecimal saldoDestinoNuevo;
    private String referencia;
    private String descripcion;
}
