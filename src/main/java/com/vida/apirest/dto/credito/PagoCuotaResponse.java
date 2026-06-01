package com.vida.apirest.dto.credito;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PagoCuotaResponse {
    private Long id;
    private Long cuotaId;
    private String cuotaNumero;
    private Long creditoId;
    private String creditoNumero;
    private BigDecimal monto;
    private String metodoPago;
    private String estado;
    private LocalDateTime createdAt;
    private LocalDateTime fechaAnulacion;
    private String motivoAnulacion;
}
