package com.vida.apirest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "afip")
public class AfipProperties {

    /** Habilita el módulo ARCA/AFIP en el servidor. */
    private boolean enabled = false;

    /** true = homologación AFIP, false = producción. */
    private boolean homologacion = true;

    /**
     * Directorio base donde cada empresa tiene su carpeta {empresaId}/ con certificados.
     * Ejemplo: ./data/afip/1/certificado.crt
     */
    private String certificadosBaseDir = "./data/afip";

    private boolean autoFacturarEnVenta = false;
    private boolean validarTokenEnLogin = true;
}
