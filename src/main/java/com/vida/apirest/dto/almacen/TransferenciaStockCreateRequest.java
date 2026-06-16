package com.vida.apirest.dto.almacen;

import lombok.Data;

import java.util.List;

@Data
public class TransferenciaStockCreateRequest {
    private Long depositoOrigenId;
    private Long depositoDestinoId;
    private String descripcion;
    private List<TransferenciaStockDetalleRequest> detalles;
}
