package com.vida.apirest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Corrige orden_de_compra.proveedor_id para que referencie proveedor(id), no tercero(id).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrdenDeCompraProveedorFkMigration implements ApplicationRunner {

    private static final List<String> LEGACY_CONSTRAINT_NAMES = List.of(
            "fk2egrdb5ykft1clcu8r621ey3m",
            "fk_odc_proveedor",
            "orden_de_compra_proveedor_id_fkey"
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tablaExiste("orden_de_compra") || !tablaExiste("proveedor")) {
                return;
            }
            if (fkCorrectaExiste() && !existeFkHaciaTercero()) {
                return;
            }

            eliminarFksHaciaTercero();
            eliminarFksHaciaProveedor();

            for (String name : LEGACY_CONSTRAINT_NAMES) {
                jdbcTemplate.execute("ALTER TABLE orden_de_compra DROP CONSTRAINT IF EXISTS " + name);
            }

            jdbcTemplate.execute("""
                    ALTER TABLE orden_de_compra
                        ADD CONSTRAINT fk_odc_proveedor
                        FOREIGN KEY (proveedor_id) REFERENCES proveedor(id)
                    """);
            log.info("FK orden_de_compra.proveedor_id -> proveedor aplicada.");
        } catch (Exception e) {
            if (fkCorrectaExiste()) {
                log.debug("FK orden_de_compra.proveedor_id -> proveedor ya vigente.");
                return;
            }
            log.warn("No se pudo aplicar FK proveedor en orden_de_compra: {}", e.getMessage());
        }
    }

    private boolean tablaExiste(String nombre) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = ?
                """,
                Integer.class,
                nombre
        );
        return count != null && count > 0;
    }

    private void eliminarFksHaciaTercero() {
        if (!tablaExiste("tercero")) {
            return;
        }
        List<String> constraints = jdbcTemplate.query("""
                SELECT c.conname
                FROM pg_constraint c
                JOIN pg_class t ON c.conrelid = t.oid
                JOIN pg_class ref ON c.confrelid = ref.oid
                WHERE t.relname = 'orden_de_compra'
                  AND c.contype = 'f'
                  AND ref.relname = 'tercero'
                """,
                (rs, rowNum) -> rs.getString("conname")
        );
        for (String name : constraints) {
            jdbcTemplate.execute("ALTER TABLE orden_de_compra DROP CONSTRAINT IF EXISTS \"" + name + "\"");
            log.info("Eliminada FK obsoleta orden_de_compra -> tercero: {}", name);
        }
    }

    private void eliminarFksHaciaProveedor() {
        List<String> constraints = jdbcTemplate.query("""
                SELECT c.conname
                FROM pg_constraint c
                JOIN pg_class t ON c.conrelid = t.oid
                JOIN pg_class ref ON c.confrelid = ref.oid
                WHERE t.relname = 'orden_de_compra'
                  AND c.contype = 'f'
                  AND ref.relname = 'proveedor'
                """,
                (rs, rowNum) -> rs.getString("conname")
        );
        for (String name : constraints) {
            jdbcTemplate.execute("ALTER TABLE orden_de_compra DROP CONSTRAINT IF EXISTS \"" + name + "\"");
        }
    }

    private boolean fkCorrectaExiste() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_constraint c
                JOIN pg_class t ON c.conrelid = t.oid
                JOIN pg_class ref ON c.confrelid = ref.oid
                WHERE t.relname = 'orden_de_compra'
                  AND c.conname = 'fk_odc_proveedor'
                  AND c.contype = 'f'
                  AND ref.relname = 'proveedor'
                """,
                Integer.class
        );
        return count != null && count > 0;
    }

    private boolean existeFkHaciaTercero() {
        if (!tablaExiste("tercero")) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_constraint c
                JOIN pg_class t ON c.conrelid = t.oid
                JOIN pg_class ref ON c.confrelid = ref.oid
                WHERE t.relname = 'orden_de_compra'
                  AND c.contype = 'f'
                  AND ref.relname = 'tercero'
                """,
                Integer.class
        );
        return count != null && count > 0;
    }
}
