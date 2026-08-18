package com.vida.apirest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    /** Si false, POST /auth/register responde 403 (recomendado en producción). */
    private boolean allowPublicRegister = false;
}
