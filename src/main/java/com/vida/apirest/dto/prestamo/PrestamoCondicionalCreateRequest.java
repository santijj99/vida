package com.vida.apirest.dto.prestamo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PrestamoCondicionalCreateRequest {
    private Long sucursalId;
    private String clienteDni;
    private Long empleadoId;
    private LocalDate fechaLimite;
    private String observaciones;
    private List<PrestamoCondicionalDetalleRequest> detalles;
}
