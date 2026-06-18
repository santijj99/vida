package com.vida.apirest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Asegura que orden_de_compra.proveedor_id referencia la tabla proveedor (no tercero).
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class OrdenDeCompraProveedorFkMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM information_schema.table_constraints
                    WHERE constraint_name = 'fk_odc_proveedor'
                    AND table_name = 'orden_de_compra'
                    """,
                    Integer.class);
            if (count != null && count > 0) {
                return;
            }

            jdbcTemplate.execute("""
                    ALTER TABLE orden_de_compra DROP CONSTRAINT IF EXISTS fk_odc_proveedor;
                    ALTER TABLE orden_de_compra DROP CONSTRAINT IF EXISTS orden_de_compra_proveedor_id_fkey;
                    ALTER TABLE orden_de_compra
                        ADD CONSTRAINT fk_odc_proveedor
                        FOREIGN KEY (proveedor_id) REFERENCES proveedor(id);
                    """);
            log.info("FK orden_de_compra.proveedor_id -> proveedor aplicada.");
        } catch (Exception e) {
            log.warn("No se pudo aplicar FK proveedor en orden_de_compra: {}", e.getMessage());
        }
    }
}
