package com.vida.apirest.dto.prestamo;

import com.vida.apirest.dto.venta.VentaCreditoPersonalRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConfirmarPrestamoCreditoRequest extends VentaCreditoPersonalRequest {
    private List<Long> prestamoDetalleIds;
}
