package com.vida.apirest.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdSecretsValidator {

    private final JwtProperties jwtProperties;
    private final LicenciaProperties licenciaProperties;
    private final Environment environment;

    @PostConstruct
    void validate() {
        ProdSecretsPolicy.assertProductionSafe(
                jwtProperties.getSecret(),
                licenciaProperties.getAesKey(),
                environment.getProperty("spring.datasource.password"));
    }
}
