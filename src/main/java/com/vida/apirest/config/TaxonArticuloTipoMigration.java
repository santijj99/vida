package com.vida.apirest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Agrega taxon_articulo.tipo para distinguir subcategoría (única) de clasificaciones (múltiples).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaxonArticuloTipoMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'taxon_articulo'
                      AND column_name = 'tipo'
                    """, Integer.class);
            if (count != null && count > 0) {
                return;
            }

            jdbcTemplate.execute("""
                    ALTER TABLE taxon_articulo
                        ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'CLASIFICACION'
                    """);

            jdbcTemplate.execute("""
                    UPDATE taxon_articulo ta
                    SET tipo = 'SUBCATEGORIA'
                    FROM (
                        SELECT articulo_id, MIN(id) AS primer_id
                        FROM taxon_articulo
                        GROUP BY articulo_id
                        HAVING COUNT(*) = 1
                    ) solo
                    WHERE ta.id = solo.primer_id
                    """);

            log.info("Columna taxon_articulo.tipo aplicada.");
        } catch (Exception e) {
            log.warn("No se pudo aplicar taxon_articulo.tipo: {}", e.getMessage());
        }
    }
}
