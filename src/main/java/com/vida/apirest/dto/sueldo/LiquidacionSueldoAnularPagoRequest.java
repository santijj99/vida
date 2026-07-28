package com.vida.apirest.dto.sueldo;

import lombok.Data;

import java.util.List;

@Data
public class LiquidacionSueldoAnularPagoRequest {
    /** Si vacío, anula todos los ítems pagados. */
    private List<Long> itemIds;
    private String observaciones;
}
