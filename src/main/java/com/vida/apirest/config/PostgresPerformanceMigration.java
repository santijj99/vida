package com.vida.apirest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Índices de personas, ventas, créditos y finanzas para prod ({@code ddl-auto: none}).
 * Complementa {@link ArticuloTablaPerformanceMigration} (catálogo).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresPerformanceMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    private static final String[] BTREE_INDEXES = {
            """
            CREATE INDEX IF NOT EXISTS ix_cliente_dni
                ON cliente (dni)
                WHERE dni IS NOT NULL
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_empleado_dni
                ON empleado (dni)
                WHERE dni IS NOT NULL
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_venta_estado
                ON venta (estado)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_venta_sucursal_fecha_estado
                ON venta (sucursal_id, fecha_venta DESC, estado)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_credito_estado
                ON credito (estado)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_credito_cliente_estado
                ON credito (cliente_id, estado)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_credito_cuenta_sucursal_activo
                ON credito_cuenta (sucursal_id, activo)
                WHERE activo = true
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_credito_cuenta_cliente_sucursal
                ON credito_cuenta (cliente_id, sucursal_id)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_cuota_estado
                ON cuota (estado)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_cuota_credito_vencimiento
                ON cuota (credito_id, fecha_vencimiento)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_gasto_sucursal_estado_created
                ON gasto (sucursal_id, estado, created_at DESC)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_odc_estado_fecha
                ON orden_de_compra (estado, fecha_orden DESC)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_pago_venta_estado
                ON pago_venta (estado)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_pago_venta_estado_venta
                ON pago_venta (estado, venta_id)
                WHERE estado = 'RECIBIDO'
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_mov_financiero_cuenta_created
                ON movimiento_financiero (cuenta_id, created_at DESC)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_caja_sesion_cuenta_estado
                ON caja_sesion (cuenta_id, estado)
                WHERE estado = 'ABIERTA'
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_taxon_articulo_articulo_taxon
                ON taxon_articulo (articulo_id, taxon_id)
            """,
    };

    private static final String[] TRGM_INDEXES = {
            """
            CREATE INDEX IF NOT EXISTS ix_cliente_nombre_trgm
                ON cliente USING gin (lower(nombre) gin_trgm_ops)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_cliente_apellido_trgm
                ON cliente USING gin (lower(apellido) gin_trgm_ops)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_cliente_dni_trgm
                ON cliente USING gin (lower(dni) gin_trgm_ops)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_cliente_nombre_completo_trgm
                ON cliente USING gin (
                    lower(COALESCE(nombre, '') || ' ' || COALESCE(apellido, '')) gin_trgm_ops
                )
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_venta_numero_factura_trgm
                ON venta USING gin (lower(numero_factura) gin_trgm_ops)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_proveedor_razon_social_trgm
                ON proveedor USING gin (lower(razon_social) gin_trgm_ops)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_proveedor_codigo_trgm
                ON proveedor USING gin (lower(codigo) gin_trgm_ops)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_gasto_descripcion_trgm
                ON gasto USING gin (lower(descripcion) gin_trgm_ops)
            """,
    };

    @Override
    public void run(ApplicationArguments args) {
        int aplicados = 0;

        for (String sql : BTREE_INDEXES) {
            if (executeIndex(sql)) {
                aplicados++;
            }
        }

        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        } catch (Exception e) {
            log.warn("No se pudo habilitar pg_trgm: {}", e.getMessage());
        }

        for (String sql : TRGM_INDEXES) {
            if (executeIndex(sql)) {
                aplicados++;
            }
        }

        log.info("Índices PostgreSQL Phase 5: {} aplicados (personas/ventas/créditos/finanzas).", aplicados);
    }

    private boolean executeIndex(String sql) {
        try {
            jdbcTemplate.execute(sql);
            return true;
        } catch (Exception e) {
            log.warn("Índice omitido: {}", e.getMessage());
            return false;
        }
    }
}
