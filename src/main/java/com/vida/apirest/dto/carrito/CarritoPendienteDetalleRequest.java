package com.vida.apirest.dto.carrito;

import lombok.Data;

@Data
public class CarritoPendienteDetalleRequest {
    private Long articuloId;
    private Long varianteId;
    private Integer cantidad;
}
