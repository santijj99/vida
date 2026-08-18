package com.vida.apirest.dto.carrito;

import com.vida.apirest.dto.afip.EmitirFacturaAFIPRequest;
import com.vida.apirest.dto.venta.PagoVentaRequest;
import com.vida.apirest.dto.venta.VentaDetalleRequest;
import lombok.Data;

import java.util.List;

@Data
public class ConfirmarCarritoPendienteRequest {
    private String metodoPago;
    private String observaciones;
    private List<VentaDetalleRequest> detalles;
    private List<PagoVentaRequest> pagos;
    /** Facturación ARCA cuando el pago es tarjeta/QR. */
    private EmitirFacturaAFIPRequest facturaAfip;
}
