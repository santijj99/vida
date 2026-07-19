package com.vida.apirest.dto.credito;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PagoCuotasResponse {
    private BigDecimal totalCuotas;
    private BigDecimal montoEntregado;
    private BigDecimal montoAplicado;
    private BigDecimal cambio;
    private boolean pagoParcial;
    private Long cuentaId;
    private List<Long> pagoIds;
    private List<CuotaCreditoResponse> cuotasActualizadas;
}
