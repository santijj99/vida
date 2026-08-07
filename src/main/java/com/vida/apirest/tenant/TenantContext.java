package com.vida.apirest.tenant;

/**
 * Contexto del tenant (código de licencia) del request actual.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CODIGO = new ThreadLocal<>();
    private static final ThreadLocal<String> DEVICE_UUID = new ThreadLocal<>();
    private static final ThreadLocal<String> DEVICE_NOMBRE = new ThreadLocal<>();

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

    /** UUID del equipo cliente (header X-Device-Uuid), no del servidor. */
    public static void setDeviceUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            DEVICE_UUID.remove();
        } else {
            DEVICE_UUID.set(uuid.trim());
        }
    }

    public static String getDeviceUuid() {
        return DEVICE_UUID.get();
    }

    /** Nombre legible del equipo cliente (header X-Device-Nombre). */
    public static void setDeviceNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            DEVICE_NOMBRE.remove();
        } else {
            DEVICE_NOMBRE.set(nombre.trim());
        }
    }

    public static String getDeviceNombre() {
        return DEVICE_NOMBRE.get();
    }

    public static void clear() {
        CODIGO.remove();
        DEVICE_UUID.remove();
        DEVICE_NOMBRE.remove();
    }
}
