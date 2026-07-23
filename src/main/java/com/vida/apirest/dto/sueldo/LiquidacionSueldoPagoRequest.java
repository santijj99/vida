package com.vida.apirest.dto.sueldo;

import lombok.Data;

import java.util.List;

@Data
public class LiquidacionSueldoPagoRequest {
    private Long cuentaId;
    /** Si vacío, paga todos los ítems pendientes de la liquidación. */
    private List<Long> itemIds;
    private String observaciones;
}
