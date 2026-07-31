package com.vida.apirest.dto.ticket;

import lombok.Data;

@Data
public class TicketConfigRequest {
    private Long empresaId;
    /** TERMICO_80MM | A4 */
    private String formato;
    private Boolean abrirAutomaticamente;

    /** Cabecera de tickets no AFIP/ARCA. */
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
