package com.vida.apirest.dto.finanzas;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCuentaFinancieraRequest {
    private Long sucursalId;
    private Long monedaId;
    private String nombre;
    private String numero;
    private String tipo; // CAJA, BANCO, TARJETA_DEBITO, TARJETA_CREDITO, BILLETERA, VIRTUAL
    private String banco;
    private BigDecimal saldoInicial;
    private String personaResponsable;
    private Boolean activo;
}
