package com.vida.apirest.dto.venta;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VentaHistorialItemResponse {
    private Long id;
    private String numeroFactura;
    private LocalDateTime fechaVenta;
    private String clienteNombre;
    private String clienteDni;
    private BigDecimal total;
    private String estado;
    private String metodoPago;
    private Integer cantidadItems;
    private String motivoCancelacion;
    /** true si la venta tiene factura ARCA autorizada (CAE). */
    @JsonProperty("facturadaArca")
    private boolean facturadaArca;
}
