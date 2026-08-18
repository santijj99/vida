package com.vida.apirest.config;

import java.util.Locale;
import java.util.Set;

/**
 * Rechaza secretos de desarrollo si el proceso corre como producción (S-06).
 */
public final class ProdSecretsPolicy {

    public static final String JWT_DEV_DEFAULT = "dev-only-change-me-before-production-min-32-chars";
    public static final String AES_DEV_DEFAULT = "0123456789abcdef0123456789abcdef";

    private static final Set<String> WEAK_DB_PASSWORDS = Set.of("1234", "postgres", "password", "admin");

    private ProdSecretsPolicy() {
    }

    public static void assertProductionSafe(String jwtSecret, String aesKey, String dbPassword) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("En prod hay que definir JWT_SECRET (mínimo 32 caracteres)");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        if (JWT_DEV_DEFAULT.equals(jwtSecret)) {
            throw new IllegalStateException("JWT_SECRET no puede ser el valor de desarrollo del YAML");
        }

        if (aesKey == null || aesKey.isBlank()) {
            throw new IllegalStateException("En prod hay que definir LICENCIA_AES_KEY (16, 24 o 32 bytes)");
        }
        if (AES_DEV_DEFAULT.equals(aesKey)) {
            throw new IllegalStateException(
                    "LICENCIA_AES_KEY no puede ser la clave de ejemplo 0123456789abcdef…");
        }
        int aesLen = aesKey.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (aesLen != 16 && aesLen != 24 && aesLen != 32) {
            throw new IllegalStateException("LICENCIA_AES_KEY debe tener 16, 24 o 32 bytes exactos");
        }

        if (dbPassword == null || dbPassword.isBlank()) {
            throw new IllegalStateException("En prod hay que definir SPRING_DATASOURCE_PASSWORD");
        }
        if (WEAK_DB_PASSWORDS.contains(dbPassword.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("SPRING_DATASOURCE_PASSWORD es demasiado débil para prod");
        }
    }
}
