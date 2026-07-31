package com.vida.apirest.config;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * DDL incremental de configuración AFIP/ARCA por empresa.
 */
@Slf4j
public final class AfipSchemaSupport {

    private AfipSchemaSupport() {
    }

    public static void apply(DataSource dataSource) {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            // Paso a paso: Hibernate falla al agregar NOT NULL sin DEFAULT en tablas con filas.
            st.execute("""
                    ALTER TABLE empresa_afip_config
                    ADD COLUMN IF NOT EXISTS homologacion BOOLEAN
                    """);
            st.execute("""
                    UPDATE empresa_afip_config
                    SET homologacion = TRUE
                    WHERE homologacion IS NULL
                    """);
            st.execute("""
                    ALTER TABLE empresa_afip_config
                    ALTER COLUMN homologacion SET DEFAULT TRUE
                    """);
            st.execute("""
                    ALTER TABLE empresa_afip_config
                    ALTER COLUMN homologacion SET NOT NULL
                    """);
            log.info("DDL AFIP empresa_afip_config.homologacion aplicado");
        } catch (Exception e) {
            log.warn("DDL AFIP (idempotente): {}", e.getMessage());
        }
    }
}
