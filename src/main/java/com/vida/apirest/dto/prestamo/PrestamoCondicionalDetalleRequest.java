package com.vida.apirest.dto.prestamo;

import lombok.Data;

@Data
public class PrestamoCondicionalDetalleRequest {
    private Long articuloId;
    private Long varianteId;
    private Integer cantidad;
}
