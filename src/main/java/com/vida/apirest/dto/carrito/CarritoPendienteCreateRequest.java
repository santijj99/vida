package com.vida.apirest.dto.carrito;

import lombok.Data;

import java.util.List;

@Data
public class CarritoPendienteCreateRequest {
    private Long sucursalId;
    private String clienteDni;
    private Long empleadoId;
    private String observaciones;
    private List<CarritoPendienteDetalleRequest> detalles;
}
