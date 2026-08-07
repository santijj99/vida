package com.vida.apirest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.licencia")
public class LicenciaProperties {

    /** Si false, el POS no consulta el servidor central (útil en desarrollo). */
    private boolean enabled = false;

    /**
     * Multi-tenant: un solo apirest enruta a la DB de cada empresa según X-Licencia-Codigo.
     * Requiere enabled=true y aes-key igual a la del servidor de licencias.
     */
    private boolean multiTenant = false;

    /** URL base del servidor de licencias, sin slash final. Ej: http://localhost:8080 */
    private String serverUrl = "http://localhost:8080";

    /** Código de licencia (modo single-tenant). En multi-tenant viene por header. */
    private String codigo = "";

    /**
     * Misma clave AES que app.encryption.aes-key del servidor licencias
     * (16, 24 o 32 bytes exactos).
     */
    private String aesKey = "0123456789abcdef0123456789abcdef";

    /**
     * UUID estable de este servidor/instalación.
     * Si está vacío, se genera y persiste en data/device-uuid.txt.
     */
    private String deviceUuid = "";

    /** Minutos que se reutiliza el último resultado sin reconsultar el servidor (info/sistema). */
    private int cacheMinutos = 28800; // 20 días

    /** Días entre revalidaciones obligatorias / gracia offline tras el último OK. */
    private int graciaDias = 20;

    /** Si true, bloquea login y operaciones cuando la licencia no es válida. */
    private boolean bloquearSiInvalida = false;
}
