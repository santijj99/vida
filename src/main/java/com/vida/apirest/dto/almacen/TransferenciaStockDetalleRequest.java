package com.vida.apirest.dto.almacen;

import lombok.Data;

@Data
public class TransferenciaStockDetalleRequest {
    private Long articuloId;
    private Long varianteId;
    private Integer cantidad;
}
