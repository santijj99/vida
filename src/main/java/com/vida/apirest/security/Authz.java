package com.vida.apirest.security;

/**
 * Expresiones {@code @PreAuthorize} para no repetir strings en controllers.
 */
public final class Authz {

    private Authz() {
    }

    public static final String GESTIONAR_VENTAS = "hasAuthority('GESTIONAR_VENTAS')";
    public static final String VER_O_GESTIONAR_VENTAS =
            "hasAnyAuthority('VER_VENTAS', 'GESTIONAR_VENTAS')";

    public static final String GESTIONAR_HISTORIAL_VENTAS = "hasAuthority('GESTIONAR_HISTORIAL_VENTAS')";
    public static final String VER_O_GESTIONAR_HISTORIAL_VENTAS =
            "hasAnyAuthority('VER_HISTORIAL_VENTAS', 'GESTIONAR_HISTORIAL_VENTAS')";
    public static final String VER_VENTA_O_HISTORIAL =
            "hasAnyAuthority('VER_VENTAS', 'GESTIONAR_VENTAS', 'VER_HISTORIAL_VENTAS', 'GESTIONAR_HISTORIAL_VENTAS')";
    public static final String VER_CAJA_O_VENTAS =
            "hasAnyAuthority('VER_CAJA', 'GESTIONAR_CAJA', 'VER_VENTAS', 'GESTIONAR_VENTAS')";

    public static final String GESTIONAR_CAJA = "hasAuthority('GESTIONAR_CAJA')";
    public static final String VER_O_GESTIONAR_CAJA =
            "hasAnyAuthority('VER_CAJA', 'GESTIONAR_CAJA')";

    public static final String GESTIONAR_ARTICULOS = "hasAuthority('GESTIONAR_ARTICULOS')";
    public static final String VER_O_GESTIONAR_ARTICULOS =
            "hasAnyAuthority('VER_ARTICULOS', 'GESTIONAR_ARTICULOS')";

    public static final String GESTIONAR_CATEGORIAS = "hasAuthority('GESTIONAR_CATEGORIAS')";
    public static final String VER_O_GESTIONAR_CATEGORIAS =
            "hasAnyAuthority('VER_CATEGORIAS', 'GESTIONAR_CATEGORIAS')";

    public static final String GESTIONAR_SUBCATEGORIAS = "hasAuthority('GESTIONAR_SUBCATEGORIAS')";
    public static final String VER_O_GESTIONAR_SUBCATEGORIAS =
            "hasAnyAuthority('VER_SUBCATEGORIAS', 'GESTIONAR_SUBCATEGORIAS')";

    public static final String GESTIONAR_TALLES = "hasAuthority('GESTIONAR_TALLES')";
    public static final String VER_O_GESTIONAR_TALLES =
            "hasAnyAuthority('VER_TALLES', 'GESTIONAR_TALLES')";

    public static final String GESTIONAR_COLORES = "hasAuthority('GESTIONAR_COLORES')";
    public static final String VER_O_GESTIONAR_COLORES =
            "hasAnyAuthority('VER_COLORES', 'GESTIONAR_COLORES')";

    public static final String GESTIONAR_GENEROS = "hasAuthority('GESTIONAR_GENEROS')";
    public static final String VER_O_GESTIONAR_GENEROS =
            "hasAnyAuthority('VER_GENEROS', 'GESTIONAR_GENEROS')";

    public static final String GESTIONAR_PROMOCIONES = "hasAuthority('GESTIONAR_PROMOCIONES')";
    public static final String VER_O_GESTIONAR_PROMOCIONES =
            "hasAnyAuthority('VER_PROMOCIONES', 'GESTIONAR_PROMOCIONES')";

    public static final String GESTIONAR_CLIENTES = "hasAuthority('GESTIONAR_CLIENTES')";
    public static final String VER_O_GESTIONAR_CLIENTES =
            "hasAnyAuthority('VER_CLIENTES', 'GESTIONAR_CLIENTES')";

    public static final String GESTIONAR_PROVEEDORES = "hasAuthority('GESTIONAR_PROVEEDORES')";
    public static final String VER_O_GESTIONAR_PROVEEDORES =
            "hasAnyAuthority('VER_PROVEEDORES', 'GESTIONAR_PROVEEDORES')";

    public static final String GESTIONAR_PEDIDOS = "hasAuthority('GESTIONAR_PEDIDOS')";
    public static final String VER_O_GESTIONAR_PEDIDOS =
            "hasAnyAuthority('VER_PEDIDOS', 'GESTIONAR_PEDIDOS')";

    public static final String GESTIONAR_CUENTAS = "hasAuthority('GESTIONAR_CUENTAS')";
    public static final String VER_O_GESTIONAR_CUENTAS =
            "hasAnyAuthority('VER_CUENTAS', 'GESTIONAR_CUENTAS')";

    public static final String GESTIONAR_ORGANIZACION = "hasAuthority('GESTIONAR_ORGANIZACION')";
    public static final String VER_O_GESTIONAR_ORGANIZACION =
            "hasAnyAuthority('VER_ORGANIZACION', 'GESTIONAR_ORGANIZACION')";

    public static final String GESTIONAR_EMPLEADOS = "hasAuthority('GESTIONAR_EMPLEADOS')";
    public static final String VER_O_GESTIONAR_EMPLEADOS =
            "hasAnyAuthority('VER_EMPLEADOS', 'GESTIONAR_EMPLEADOS')";

    public static final String GESTIONAR_ARCA = "hasAuthority('GESTIONAR_ARCA')";
    public static final String VER_O_GESTIONAR_ARCA =
            "hasAnyAuthority('VER_ARCA', 'GESTIONAR_ARCA')";

    public static final String VER_O_GESTIONAR_ORG_O_CAJA =
            "hasAnyAuthority('VER_ORGANIZACION', 'GESTIONAR_ORGANIZACION', 'VER_CAJA', 'GESTIONAR_CAJA')";
    public static final String GESTIONAR_CAJA_O_ORGANIZACION =
            "hasAnyAuthority('GESTIONAR_CAJA', 'GESTIONAR_ORGANIZACION')";
}
