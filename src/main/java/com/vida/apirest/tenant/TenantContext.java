package com.vida.apirest.tenant;

/**
 * Contexto del tenant (código de licencia) del request actual.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CODIGO = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCodigoLicencia(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            CODIGO.remove();
        } else {
            CODIGO.set(codigo.trim());
        }
    }

    public static String getCodigoLicencia() {
        return CODIGO.get();
    }

    public static void clear() {
        CODIGO.remove();
    }
}
