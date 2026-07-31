package com.vida.apirest.dto.ticket;

import lombok.Data;

@Data
public class TicketConfigResponse {
    private Long id;
    private Long empresaId;
    private String empresaNombre;
    private String formato;
    private Boolean abrirAutomaticamente;

    /** Cabecera usada en comprobantes no fiscales (venta, crédito, cobro). */
    private String cabeceraRazonSocial;
    private String cabeceraDireccion;
    private String cabeceraCuit;
    private String cabeceraCondicionIva;
    private String cabeceraIibb;
    private String cabeceraInicioActividad;
    /** true = "Razón social: Nombre"; false = solo el nombre. */
    private Boolean cabeceraMostrarEtiquetaRazonSocial;
    private Boolean cabeceraMostrarDireccion;
    private Boolean cabeceraMostrarCuit;
    private Boolean cabeceraMostrarCondicionIva;
}
