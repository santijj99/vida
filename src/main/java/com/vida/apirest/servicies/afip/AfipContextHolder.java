package com.vida.apirest.servicies.afip;

public final class AfipContextHolder {

    private static final ThreadLocal<AfipContext> CURRENT = new ThreadLocal<>();

    private AfipContextHolder() {
    }

    public static void set(AfipContext context) {
        CURRENT.set(context);
    }

    public static AfipContext get() {
        return CURRENT.get();
    }

    public static AfipContext require() {
        AfipContext ctx = CURRENT.get();
        if (ctx == null) {
            throw new IllegalStateException("No hay contexto AFIP activo para la empresa");
        }
        return ctx;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
