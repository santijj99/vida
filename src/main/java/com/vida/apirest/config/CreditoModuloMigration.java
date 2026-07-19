package com.vida.apirest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Esquema del módulo de créditos ampliado: config por empresa, recargo en cuota, historial.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditoModuloMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ejecutar("""
                CREATE TABLE IF NOT EXISTS credito_config_empresa (
                    id BIGSERIAL PRIMARY KEY,
                    empresa_id BIGINT NOT NULL UNIQUE REFERENCES empresa(id),
                    dias_gracia INTEGER NOT NULL DEFAULT 0,
                    porcentaje_mora NUMERIC(9,4) NOT NULL DEFAULT 10,
                    tipo_interes VARCHAR(20) NOT NULL DEFAULT 'FIJO',
                    modo_dia_vencimiento VARCHAR(30) NOT NULL DEFAULT 'DIA_10',
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);
        ejecutar("""
                CREATE TABLE IF NOT EXISTS credito_historial (
                    id BIGSERIAL PRIMARY KEY,
                    credito_id BIGINT NOT NULL REFERENCES credito(id),
                    campo VARCHAR(100) NOT NULL,
                    valor_anterior TEXT,
                    valor_nuevo TEXT,
                    usuario_id BIGINT,
                    usuario_nombre VARCHAR(200),
                    created_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);
        ejecutar("""
                CREATE INDEX IF NOT EXISTS ix_credito_historial_credito
                    ON credito_historial (credito_id, created_at DESC)
                """);
        ejecutar("""
                ALTER TABLE cuota ADD COLUMN IF NOT EXISTS recargo NUMERIC(15,2) DEFAULT 0
                """);
        ejecutar("""
                ALTER TABLE cuota ADD COLUMN IF NOT EXISTS recargo_exento BOOLEAN DEFAULT FALSE
                """);
        ejecutar("""
                UPDATE cuota SET recargo_exento = FALSE WHERE recargo_exento IS NULL
                """);
        ejecutar("""
                UPDATE cuota SET recargo = 0 WHERE recargo IS NULL
                """);
        log.info("Migración módulo créditos aplicada");
    }

    private void ejecutar(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.warn("Migración créditos (puede ser idempotente): {}", e.getMessage());
        }
    }
}
