package com.vida.apirest.dto.credito;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PagoCuotasRequest {
    private List<Long> cuotaIds;
    private BigDecimal montoEntregado;
    private String metodoPago;
    private Long cuentaFinancieraId;
}
