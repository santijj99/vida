package com.vida.apirest.config;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * DDL + seed RBAC del módulo sueldos, reutilizable en DB default y en cada tenant.
 */
@Slf4j
public final class SueldoSchemaSupport {

    private SueldoSchemaSupport() {
    }

    public static void apply(DataSource dataSource) {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS empleado_sueldo_config (
                        id BIGSERIAL PRIMARY KEY,
                        empleado_id BIGINT NOT NULL UNIQUE REFERENCES empleado(id),
                        sueldo_fijo NUMERIC(15,2) NOT NULL DEFAULT 0,
                        periodo_base VARCHAR(20) NOT NULL DEFAULT 'MES',
                        porcentaje_comision NUMERIC(9,4) NOT NULL DEFAULT 0,
                        activo BOOLEAN NOT NULL DEFAULT TRUE,
                        observaciones TEXT,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS liquidacion_sueldo (
                        id BIGSERIAL PRIMARY KEY,
                        numero VARCHAR(50) NOT NULL UNIQUE,
                        sucursal_id BIGINT REFERENCES sucursal(id),
                        periodo_tipo VARCHAR(20) NOT NULL,
                        fecha_desde DATE NOT NULL,
                        fecha_hasta DATE NOT NULL,
                        porcentaje_comision_override NUMERIC(9,4),
                        estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
                        total_sueldos NUMERIC(15,2) NOT NULL DEFAULT 0,
                        total_comisiones NUMERIC(15,2) NOT NULL DEFAULT 0,
                        total_general NUMERIC(15,2) NOT NULL DEFAULT 0,
                        responsable VARCHAR(255),
                        observaciones TEXT,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                    )
                    """);
            st.execute("CREATE INDEX IF NOT EXISTS ix_liq_sueldo_estado ON liquidacion_sueldo (estado)");
            st.execute("CREATE INDEX IF NOT EXISTS ix_liq_sueldo_fechas ON liquidacion_sueldo (fecha_desde, fecha_hasta)");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS liquidacion_sueldo_item (
                        id BIGSERIAL PRIMARY KEY,
                        liquidacion_id BIGINT NOT NULL REFERENCES liquidacion_sueldo(id) ON DELETE CASCADE,
                        empleado_id BIGINT NOT NULL REFERENCES empleado(id),
                        sueldo_base NUMERIC(15,2) NOT NULL DEFAULT 0,
                        ventas_total NUMERIC(15,2) NOT NULL DEFAULT 0,
                        cantidad_ventas INTEGER NOT NULL DEFAULT 0,
                        porcentaje_comision NUMERIC(9,4) NOT NULL DEFAULT 0,
                        comision_monto NUMERIC(15,2) NOT NULL DEFAULT 0,
                        total NUMERIC(15,2) NOT NULL DEFAULT 0,
                        estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                        cuenta_pago_id BIGINT REFERENCES cuenta_financiera(id),
                        movimiento_id BIGINT REFERENCES movimiento_financiero(id),
                        fecha_pago TIMESTAMP,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                    )
                    """);
            st.execute("CREATE INDEX IF NOT EXISTS ix_liq_item_liquidacion ON liquidacion_sueldo_item (liquidacion_id)");
            st.execute("CREATE INDEX IF NOT EXISTS ix_liq_item_empleado ON liquidacion_sueldo_item (empleado_id)");
            st.execute("""
                    ALTER TABLE liquidacion_sueldo_item
                        ADD COLUMN IF NOT EXISTS cantidad_articulos INTEGER NOT NULL DEFAULT 0
                    """);
        } catch (Exception e) {
            log.warn("DDL sueldos (idempotente): {}", e.getMessage());
        }

        seedPermiso(dataSource, "VER_SUELDOS", "Finanzas", "Ver sueldos",
                "Liquidaciones y comisiones de empleados");
        seedPermiso(dataSource, "GESTIONAR_SUELDOS", "Finanzas", "Gestionar sueldos",
                "Configurar, liquidar y pagar sueldos/comisiones");
    }

    private static void seedPermiso(
            DataSource dataSource,
            String codigo,
            String modulo,
            String nombre,
            String descripcion
    ) {
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO permiso (codigo, modulo, nombre, descripcion)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (codigo) DO UPDATE SET
                        modulo = EXCLUDED.modulo,
                        nombre = EXCLUDED.nombre,
                        descripcion = EXCLUDED.descripcion
                    """)) {
                ps.setString(1, codigo);
                ps.setString(2, modulo);
                ps.setString(3, nombre);
                ps.setString(4, descripcion);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO role_permiso (role_id, permiso_id)
                    SELECT r.id, p.id
                    FROM roles r
                    CROSS JOIN permiso p
                    WHERE UPPER(TRIM(r.nombre)) = 'ADMINISTRADOR' AND p.codigo = ?
                      AND NOT EXISTS (
                        SELECT 1 FROM role_permiso rp
                        WHERE rp.role_id = r.id AND rp.permiso_id = p.id
                      )
                    """)) {
                ps.setString(1, codigo);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            log.warn("Seed permiso {}: {}", codigo, e.getMessage());
        }
    }
}
