package com.vida.apirest.dto.prestamo;

import lombok.Data;

import java.util.List;

@Data
public class DevolverPrestamoDetallesRequest {
    private List<Long> detalleIds;
}
