package com.vida.apirest.dto.prestamo;

import com.vida.apirest.dto.venta.PagoVentaRequest;
import com.vida.apirest.dto.venta.VentaDetalleRequest;
import lombok.Data;

import java.util.List;

@Data
public class ConfirmarPrestamoRequest {
    /** IDs de líneas del préstamo a confirmar (parcial o total). */
    private List<Long> prestamoDetalleIds;
    private String metodoPago;
    private String observaciones;
    private List<VentaDetalleRequest> detalles;
    private List<PagoVentaRequest> pagos;
}
