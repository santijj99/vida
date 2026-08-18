package com.vida.apirest.dto.venta;

import com.vida.apirest.dto.afip.EmitirFacturaAFIPRequest;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VentaCreateRequest {
    private Long sucursalId;
    private Long empleadoId;
    private String clienteDni;
    private String numeroFactura;
    private LocalDateTime fechaVenta;
    private String observaciones;
    private String metodoPago;
    private List<VentaDetalleRequest> detalles;
    private List<PagoVentaRequest> pagos;
    /** Datos de facturación ARCA/AFIP cuando el pago lo requiere. */
    private EmitirFacturaAFIPRequest facturaAfip;
    /**
     * Id estable del cliente (p. ej. UUID local del outbox offline).
     * Si se reenvía el mismo valor, se devuelve la venta ya creada sin duplicar stock/caja.
     */
    private String clientRequestId;
}
