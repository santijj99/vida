package com.vida.apirest.dto.credito;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreditoHistorialResponse {
    private Long id;
    private Long creditoId;
    private String campo;
    private String valorAnterior;
    private String valorNuevo;
    private Long usuarioId;
    private String usuarioNombre;
    private LocalDateTime createdAt;
}
