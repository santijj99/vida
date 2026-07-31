package com.vida.apirest.config;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * DDL de configuración de tickets PDF (formato + apertura automática),
 * reutilizable en DB default y en cada tenant.
 */
@Slf4j
public final class TicketSchemaSupport {

    private TicketSchemaSupport() {
    }

    public static void apply(DataSource dataSource) {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS empresa_ticket_config (
                        id BIGSERIAL PRIMARY KEY,
                        empresa_id BIGINT NOT NULL UNIQUE REFERENCES empresa(id),
                        formato VARCHAR(20) NOT NULL DEFAULT 'TERMICO_80MM',
                        abrir_automaticamente BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                    )
                    """);
            st.execute("""
                    ALTER TABLE empresa_ticket_config
                    ADD COLUMN IF NOT EXISTS abrir_automaticamente BOOLEAN NOT NULL DEFAULT TRUE
                    """);
            st.execute("""
                    ALTER TABLE empresa_ticket_config
                    ADD COLUMN IF NOT EXISTS cabecera_razon_social VARCHAR(255)
                    """);
            st.execute("""
                    ALTER TABLE empresa_ticket_config
                    ADD COLUMN IF NOT EXISTS cabecera_direccion VARCHAR(500)
                    """);
            st.execute("""
                    ALTER TABLE empresa_ticket_config
                    ADD COLUMN IF NOT EXISTS cabecera_cuit VARCHAR(20)
                    """);
            st.execute("""
                    ALTER TABLE empresa_ticket_config
                    ADD COLUMN IF NOT EXISTS cabecera_condicion_iva VARCHAR(120)
                    """);
            st.execute("""
                    ALTER TABLE empresa_ticket_config
                    ADD COLUMN IF NOT EXISTS cabecera_iibb VARCHAR(80)
                    """);
            st.execute("""
                    ALTER TABLE empresa_ticket_config
                    ADD COLUMN IF NOT EXISTS cabecera_inicio_actividad VARCHAR(40)
                    """);
            st.execute("""
                    ALTER TABLE empresa_ticket_config
                    ADD COLUMN IF NOT EXISTS cabecera_mostrar_etiqueta_razon_social BOOLEAN NOT NULL DEFAULT TRUE
                    """);
            st.execute("""
                    ALTER TABLE empresa_ticket_config
                    ADD COLUMN IF NOT EXISTS cabecera_mostrar_direccion BOOLEAN NOT NULL DEFAULT TRUE
                    """);
            st.execute("""
                    ALTER TABLE empresa_ticket_config
                    ADD COLUMN IF NOT EXISTS cabecera_mostrar_cuit BOOLEAN NOT NULL DEFAULT TRUE
                    """);
            st.execute("""
                    ALTER TABLE empresa_ticket_config
                    ADD COLUMN IF NOT EXISTS cabecera_mostrar_condicion_iva BOOLEAN NOT NULL DEFAULT TRUE
                    """);
            log.info("DDL tickets aplicado");
        } catch (Exception e) {
            log.warn("DDL tickets (idempotente): {}", e.getMessage());
        }
    }
}
