package com.vida.apirest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reemplaza persona_responsable (texto) por empleado_id (FK) en cuenta_financiera.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CuentaFinancieraEmpleadoFkMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE cuenta_financiera ADD COLUMN IF NOT EXISTS empleado_id BIGINT;
                    ALTER TABLE cuenta_financiera DROP COLUMN IF EXISTS persona_responsable;
                    """);

            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM information_schema.table_constraints
                    WHERE constraint_name = 'fk_cuenta_empleado_responsable'
                    AND table_name = 'cuenta_financiera'
                    """,
                    Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.execute("""
                        ALTER TABLE cuenta_financiera
                            ADD CONSTRAINT fk_cuenta_empleado_responsable
                            FOREIGN KEY (empleado_id) REFERENCES empleado(id);
                        """);
            }
            log.info("Migración cuenta_financiera.empleado_id aplicada.");
        } catch (Exception e) {
            log.warn("No se pudo aplicar migración empleado_id en cuenta_financiera: {}", e.getMessage());
        }
    }
}
