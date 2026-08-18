package com.vida.apirest.config;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
public final class UsuarioSchemaSupport {

    private UsuarioSchemaSupport() {
    }

    public static void apply(DataSource dataSource) {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute("""
                    ALTER TABLE usuario
                        ADD COLUMN IF NOT EXISTS debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE
                    """);
            st.execute("""
                    ALTER TABLE usuario
                        ADD COLUMN IF NOT EXISTS es_soporte BOOLEAN NOT NULL DEFAULT FALSE
                    """);
            st.execute("""
                    ALTER TABLE usuario
                        ADD COLUMN IF NOT EXISTS soporte_expira_at TIMESTAMPTZ
                    """);
            st.execute("""
                    ALTER TABLE usuario
                        ADD COLUMN IF NOT EXISTS soporte_token_hash VARCHAR(64)
                    """);
            st.execute("""
                    ALTER TABLE usuario
                        ALTER COLUMN reset_codigo TYPE VARCHAR(64)
                    """);
            st.execute("""
                    ALTER TABLE usuario
                        ADD COLUMN IF NOT EXISTS reset_intentos INTEGER NOT NULL DEFAULT 0
                    """);
            // En dos pasos: Hibernate ddl-auto intenta ADD ... NOT NULL sin DEFAULT y
            // PostgreSQL lo rechaza si ya hay filas. Primero nullable + default, luego NOT NULL.
            st.execute("""
                    ALTER TABLE usuario
                        ADD COLUMN IF NOT EXISTS token_version INTEGER DEFAULT 0
                    """);
            st.execute("UPDATE usuario SET token_version = 0 WHERE token_version IS NULL");
            st.execute("ALTER TABLE usuario ALTER COLUMN token_version SET DEFAULT 0");
            st.execute("ALTER TABLE usuario ALTER COLUMN token_version SET NOT NULL");
        } catch (Exception ex) {
            log.warn("No se pudo aplicar DDL de usuario (password/soporte/token_version): {}", ex.getMessage());
        }
    }
}
