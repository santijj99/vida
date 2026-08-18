package com.vida.apirest.config;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * DDL de idempotencia de ventas (client_request_id), reutilizable en DB default y tenants.
 */
@Slf4j
public final class VentaSchemaSupport {

    private VentaSchemaSupport() {
    }

    public static void apply(DataSource dataSource) {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute("""
                    ALTER TABLE venta
                    ADD COLUMN IF NOT EXISTS client_request_id VARCHAR(96)
                    """);
            st.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS uk_venta_client_request_id
                    ON venta (client_request_id)
                    """);
            log.info("DDL venta.client_request_id aplicado");
        } catch (Exception e) {
            log.warn("DDL venta.client_request_id (idempotente): {}", e.getMessage());
        }
    }
}
