package com.vida.apirest.model.auth;

import java.util.List;

/**
 * Permisos funcionales alineados con {@code @PreAuthorize} y visibilidad del menú lateral.
 */
public final class PermisoCodigo {

    private PermisoCodigo() {
    }

    /** GET /api/stock */
    public static final String LEER_STOCK = "LEER_STOCK";

    /** DELETE /api/stock/{id} */
    public static final String ELIMINAR_STOCK = "ELIMINAR_STOCK";

    /** POST /api/transferencias-stock */
    public static final String TRANSFERIR_STOCK = "TRANSFERIR_STOCK";

    /** Menú transferencias de stock */
    public static final String VER_TRANSFERENCIAS_STOCK = "VER_TRANSFERENCIAS_STOCK";

    /** GET /usuario */
    public static final String LEER_USUARIOS = "LEER_USUARIOS";

    /** POST /usuario/admin/create */
    public static final String CREAR_USUARIOS = "CREAR_USUARIOS";

    /** POST /usuario/{id}/asignar-rol/{rolId} */
    public static final String MODIFICAR_USUARIOS = "MODIFICAR_USUARIOS";

    /** GET/PUT /api/rbac/** — incluye sección Accesos del menú */
    public static final String ADMINISTRAR_PERMISOS = "ADMINISTRAR_PERMISOS";

    // --- Navegación (sidebar) ---

    public static final String VER_DASHBOARD = "VER_DASHBOARD";
    public static final String VER_VENTAS = "VER_VENTAS";
    public static final String VER_HISTORIAL_VENTAS = "VER_HISTORIAL_VENTAS";
    public static final String VER_CAJA = "VER_CAJA";
    public static final String VER_ARTICULOS = "VER_ARTICULOS";
    public static final String VER_CATEGORIAS = "VER_CATEGORIAS";
    public static final String VER_SUBCATEGORIAS = "VER_SUBCATEGORIAS";
    public static final String VER_TALLES = "VER_TALLES";
    public static final String VER_COLORES = "VER_COLORES";
    public static final String VER_GENEROS = "VER_GENEROS";
    public static final String VER_PROMOCIONES = "VER_PROMOCIONES";
    public static final String VER_CLIENTES = "VER_CLIENTES";
    public static final String VER_PROVEEDORES = "VER_PROVEEDORES";
    public static final String VER_PEDIDOS = "VER_PEDIDOS";
    public static final String VER_CUENTAS = "VER_CUENTAS";
    public static final String CONFIGURAR_CREDITOS = "CONFIGURAR_CREDITOS";
    public static final String EDITAR_CREDITOS = "EDITAR_CREDITOS";
    public static final String VER_GASTOS = "VER_GASTOS";
    public static final String GESTIONAR_GASTOS = "GESTIONAR_GASTOS";
    public static final String VER_ORGANIZACION = "VER_ORGANIZACION";
    public static final String VER_EMPLEADOS = "VER_EMPLEADOS";
    public static final String VER_ARCA = "VER_ARCA";

    public static List<String> todos() {
        return List.of(
                LEER_STOCK,
                ELIMINAR_STOCK,
                TRANSFERIR_STOCK,
                VER_TRANSFERENCIAS_STOCK,
                LEER_USUARIOS,
                CREAR_USUARIOS,
                MODIFICAR_USUARIOS,
                ADMINISTRAR_PERMISOS,
                VER_DASHBOARD,
                VER_VENTAS,
                VER_HISTORIAL_VENTAS,
                VER_CAJA,
                VER_ARTICULOS,
                VER_CATEGORIAS,
                VER_SUBCATEGORIAS,
                VER_TALLES,
                VER_COLORES,
                VER_GENEROS,
                VER_PROMOCIONES,
                VER_CLIENTES,
                VER_PROVEEDORES,
                VER_PEDIDOS,
                VER_CUENTAS,
                CONFIGURAR_CREDITOS,
                EDITAR_CREDITOS,
                VER_GASTOS,
                GESTIONAR_GASTOS,
                VER_ORGANIZACION,
                VER_TRANSFERENCIAS_STOCK,
                VER_EMPLEADOS,
                VER_ARCA
        );
    }

    public static List<String> navegacion() {
        return List.of(
                VER_DASHBOARD,
                VER_VENTAS,
                VER_HISTORIAL_VENTAS,
                VER_CAJA,
                VER_ARTICULOS,
                VER_CATEGORIAS,
                VER_SUBCATEGORIAS,
                VER_TALLES,
                VER_COLORES,
                VER_GENEROS,
                VER_PROMOCIONES,
                VER_CLIENTES,
                VER_PROVEEDORES,
                VER_PEDIDOS,
                VER_CUENTAS,
                CONFIGURAR_CREDITOS,
                EDITAR_CREDITOS,
                VER_GASTOS,
                GESTIONAR_GASTOS,
                VER_ORGANIZACION,
                VER_TRANSFERENCIAS_STOCK,
                VER_EMPLEADOS,
                VER_ARCA
        );
    }
}
