package com.vida.apirest.servicies.afip;

import java.nio.file.Path;

/**
 * Datos fiscales y rutas de certificados de una empresa para operar con ARCA/AFIP.
 */
public record AfipContext(
        Long empresaId,
        String cuit,
        String razonSocial,
        String direccion,
        String condicionIva,
        String iibb,
        String inicioActividad,
        int ptoVta,
        int cbteTipoDefault,
        boolean homologacion,
        Path certificadosDir,
        String clavePrivadaPassword
) {
    public String cuitSinGuiones() {
        if (cuit == null) {
            return "";
        }
        return cuit.replace("-", "").replace(" ", "").trim();
    }
}
