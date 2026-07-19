package com.vida.apirest.dto.almacen;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TransferenciaStockResponse {
    private Long id;
    private String numero;
    private Long depositoOrigenId;
    private String depositoOrigenNombre;
    private Long sucursalOrigenId;
    private String sucursalOrigenNombre;
    private Long depositoDestinoId;
    private String depositoDestinoNombre;
    private Long sucursalDestinoId;
    private String sucursalDestinoNombre;
    private String estado;
    private String descripcion;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaRecepcion;
    private LocalDateTime createdAt;
    private Integer totalUnidades;
    private List<TransferenciaStockDetalleResponse> detalles;
}
