package com.vida.apirest.dto.credito;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreditoClienteResponse {
    private Long id;
    private String numero;
    private Integer indice;
    private BigDecimal importe;
    private BigDecimal saldo;
    private LocalDateTime fechaVencimiento;
    private String estado;
    private String descripcion;
    private Long ventaId;
    private String numeroFactura;
    private LocalDateTime createdAt;
    private List<CuotaCreditoResponse> cuotas;
    private Integer diasAtraso;
    private BigDecimal recargoAcumulado;
}
