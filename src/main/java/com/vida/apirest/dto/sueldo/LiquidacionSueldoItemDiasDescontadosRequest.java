package com.vida.apirest.dto.sueldo;

import lombok.Data;

@Data
public class LiquidacionSueldoItemDiasDescontadosRequest {
    /** Cantidad de días a descontar (≥ 0). */
    private Integer diasDescontados;
}
