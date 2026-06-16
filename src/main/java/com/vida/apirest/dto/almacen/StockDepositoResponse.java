package com.vida.apirest.dto.almacen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDepositoResponse {
    private Long stockId;
    private Long articuloId;
    private Long varianteId;
    private String codigo;
    private String marca;
    private String modelo;
    private String talle;
    private String color;
    private String codigoBarras;
    private Integer stockDisponible;
}
