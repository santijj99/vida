package com.vida.apirest.dto.finanzas;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferenciaCuentaRequest {
    private Long cuentaOrigenId;
    private Long cuentaDestinoId;
    private BigDecimal monto;
    private String descripcion;
}
