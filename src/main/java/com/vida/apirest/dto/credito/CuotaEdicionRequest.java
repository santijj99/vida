package com.vida.apirest.dto.credito;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CuotaEdicionRequest {
    private Long id;
    private String numero;
    private BigDecimal monto;
    private BigDecimal saldo;
    private LocalDateTime fechaVencimiento;
    private String estado;
    private Boolean quitarRecargo;
}
