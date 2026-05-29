package com.vida.apirest.repositories;

import java.math.BigDecimal;

/**
 * Proyección agregada de créditos por cliente (excluye cancelados).
 */
public interface CreditoResumenPorCliente {
    Long getClienteId();

    BigDecimal getTotalCreditosSacados();

    BigDecimal getTotalPagado();

    Long getCantidadCreditos();
}
