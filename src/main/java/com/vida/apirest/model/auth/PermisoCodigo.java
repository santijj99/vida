package com.vida.apirest.model.auth;

import java.util.List;

/**
 * Permisos funcionales alineados con {@code @PreAuthorize} en los controllers.
 * Solo incluye operaciones actualmente protegidas.
 */
public final class PermisoCodigo {

    private PermisoCodigo() {
    }

    /** GET /api/stock */
    public static final String LEER_STOCK = "LEER_STOCK";

    /** DELETE /api/stock/{id} */
    public static final String ELIMINAR_STOCK = "ELIMINAR_STOCK";

    /** GET /usuario */
    public static final String LEER_USUARIOS = "LEER_USUARIOS";

    /** POST /usuario/admin/create */
    public static final String CREAR_USUARIOS = "CREAR_USUARIOS";

    /** POST /usuario/{id}/asignar-rol/{rolId} */
    public static final String MODIFICAR_USUARIOS = "MODIFICAR_USUARIOS";

    /** GET/PUT /api/rbac/** */
    public static final String ADMINISTRAR_PERMISOS = "ADMINISTRAR_PERMISOS";

    public static List<String> todos() {
        return List.of(
                LEER_STOCK,
                ELIMINAR_STOCK,
                LEER_USUARIOS,
                CREAR_USUARIOS,
                MODIFICAR_USUARIOS,
                ADMINISTRAR_PERMISOS
        );
    }
}
