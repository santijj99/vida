package com.vida.apirest.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockResponse {
    private Long id;
    private Long articuloId;
    private String articuloCodigo;
    private Long varianteId;
    private String varianteDescripcion;
    private Long sucursalId;
    private String sucursalNombre;
    private Integer cantidadActual;
    private Integer cantidadDisponible;
    private Integer cantidadReservada;
}
