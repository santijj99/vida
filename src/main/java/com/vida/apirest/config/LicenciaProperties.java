package com.vida.apirest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

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
    private String aesKey = "";

    /**
     * UUID estable de este servidor/instalación.
     * Si está vacío, se genera y persiste en data/device-uuid.txt.
     */
    private String deviceUuid = "";

    /** Minutos entre revalidaciones con el servidor online (S-11: no usar la gracia). */
    private int cacheMinutos = 360;

    /** Días de operación offline solo si el servidor de licencias está caído. */
    private int graciaDias = 20;

    /** Si true, bloquea login cuando la licencia no es válida (single-tenant). En multi-tenant el pool se cierra igual. */
    private boolean bloquearSiInvalida = false;

    public Duration revalidacionOnline() {
        return Duration.ofMinutes(Math.max(1, cacheMinutos));
    }

    public Duration graciaOffline() {
        return Duration.ofDays(Math.max(0, graciaDias));
    }

    public Duration reintentoSiServidorCaido() {
        return Duration.ofHours(6);
    }
}
