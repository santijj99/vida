package com.vida.apirest.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdSecretsPolicyTest {

    @Test
    void aceptaSecretosFuertes() {
        assertDoesNotThrow(() -> ProdSecretsPolicy.assertProductionSafe(
                "abcdefghijklmnopqrstuvwxyz012345",
                "12345678901234567890123456789012",
                "s3gura-no-1234"));
    }

    @Test
    void rechazaJwtDeDesarrollo() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ProdSecretsPolicy.assertProductionSafe(
                        ProdSecretsPolicy.JWT_DEV_DEFAULT,
                        "12345678901234567890123456789012",
                        "s3gura"));
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void rechazaAesDeEjemplo() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ProdSecretsPolicy.assertProductionSafe(
                        "abcdefghijklmnopqrstuvwxyz012345",
                        ProdSecretsPolicy.AES_DEV_DEFAULT,
                        "s3gura"));
        assertTrue(ex.getMessage().contains("LICENCIA_AES_KEY"));
    }

    @Test
    void rechazaPassword1234() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ProdSecretsPolicy.assertProductionSafe(
                        "abcdefghijklmnopqrstuvwxyz012345",
                        "12345678901234567890123456789012",
                        "1234"));
        assertTrue(ex.getMessage().contains("SPRING_DATASOURCE_PASSWORD"));
    }
}
