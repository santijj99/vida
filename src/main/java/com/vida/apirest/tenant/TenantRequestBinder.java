package com.vida.apirest.tenant;

/**
 * Resuelve el código de tenant de un request.
 * Con JWT, el claim {@code licencia} manda: el header no puede cambiar de empresa.
 * Login / forgot / reset siguen usando solo el header (todavía no hay token).
 */
public final class TenantRequestBinder {

    public enum Error {
        MISSING_HEADER,
        MISSING_CLAIM,
        MISMATCH
    }

    public record Resolution(String codigo, Error error) {
        public boolean ok() {
            return error == null && codigo != null && !codigo.isBlank();
        }
    }

    private TenantRequestBinder() {
    }

    public static Resolution resolve(
            String header,
            String jwtClaim,
            boolean bearerPresent,
            boolean publicAuth
    ) {
        String headerNorm = normalize(header);
        String claimNorm = normalize(jwtClaim);

        if (publicAuth) {
            if (headerNorm == null) {
                return new Resolution(null, Error.MISSING_HEADER);
            }
            return new Resolution(headerNorm, null);
        }

        if (bearerPresent) {
            if (claimNorm == null) {
                return new Resolution(null, Error.MISSING_CLAIM);
            }
            if (headerNorm != null && !headerNorm.equals(claimNorm)) {
                return new Resolution(null, Error.MISMATCH);
            }
            return new Resolution(claimNorm, null);
        }

        if (headerNorm == null) {
            return new Resolution(null, Error.MISSING_HEADER);
        }
        return new Resolution(headerNorm, null);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
