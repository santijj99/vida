package com.vida.apirest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketModuloMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ejecutar("""
                CREATE TABLE IF NOT EXISTS empresa_ticket_config (
                    id BIGSERIAL PRIMARY KEY,
                    empresa_id BIGINT NOT NULL UNIQUE REFERENCES empresa(id),
                    formato VARCHAR(20) NOT NULL DEFAULT 'TERMICO_80MM',
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);
        log.info("Migración configuración tickets aplicada");
    }

    private void ejecutar(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.warn("Migración tickets (puede ser idempotente): {}", e.getMessage());
        }
    }
}
