package com.vida.apirest.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret = "";
    private long expirationHours = 24;

    @PostConstruct
    void validate() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret debe tener al menos 32 caracteres (variable JWT_SECRET)");
        }
    }
}
