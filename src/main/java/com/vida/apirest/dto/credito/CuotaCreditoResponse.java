package com.vida.apirest.dto.credito;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CuotaCreditoResponse {
    private Long id;
    private Long creditoId;
    private String numero;
    private LocalDateTime fechaVencimiento;
    private BigDecimal monto;
    private BigDecimal pagoRealizado;
    private BigDecimal saldo;
    private String estado;
    private BigDecimal recargo;
    private Integer diasAtraso;
    private String descripcion;
}
