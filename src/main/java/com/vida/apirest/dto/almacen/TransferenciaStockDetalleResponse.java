package com.vida.apirest.dto.almacen;

import lombok.Data;

@Data
public class TransferenciaStockDetalleResponse {
    private Long id;
    private Long articuloId;
    private Long varianteId;
    private String codigo;
    private String descripcion;
    private String talle;
    private String color;
    private Integer cantidadEnviada;
    private Integer cantidadRecibida;
}
