package com.vida.apirest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "afip")
public class AfipProperties {

    private boolean enabled = false;
    private boolean homologacion = true;
    private String cuit = "";
    private int ptoVta = 3;
    private int cbteTipo = 6;
    private String tokenXmlPath = "classpath:certificados/TA.xml";
    private String certPath = "";
    private String certPassword = "";
    private String phpScriptPath = "";
    /** Carpeta testing/homologación (wsaa-client.php + certificado.crt + TA.xml). */
    private String phpScriptPathHomologacion = "";
    private boolean autoFacturarEnVenta = true;
    private boolean validarTokenEnLogin = true;
    private Empresa empresa = new Empresa();

    @Data
    public static class Empresa {
        private String razonSocial = "";
        private String direccion = "";
        private String cuit = "";
        private String condicionIva = "IVA Responsable Inscripto";
        private String iibb = "";
        private String inicioActividad = "";
    }
}
