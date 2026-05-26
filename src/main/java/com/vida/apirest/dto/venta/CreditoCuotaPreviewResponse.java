package com.vida.apirest.dto.venta;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditoCuotaPreviewResponse {
    private Integer numero;
    private String etiqueta;
    private LocalDateTime fechaVencimiento;
    private BigDecimal monto;
    private BigDecimal saldo;
    private String estado;
    private String descripcion;
    private Boolean anticipo;
    private Boolean pagadaAlCrear;
}
