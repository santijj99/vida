package com.vida.apirest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Índices del catálogo que JPA no puede declarar en {@code @Index}:
 * extensión pg_trgm, GIN sobre expresiones y índices parciales.
 * <p>
 * Los índices B-tree están definidos en las entidades ({@code Stock}, {@code HistorialPrecio},
 * {@code Articulo}, {@code VarianteArticulo}, etc.). En dev Hibernate los crea con
 * {@code ddl-auto: update}. En prod ({@code ddl-auto: none}) se sincronizan aquí.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticuloTablaPerformanceMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    /** Réplica de @Index en entidades — necesario en prod (ddl-auto: none). */
    private static final String[] ENTITY_BTREE_INDEXES = {
            """
            CREATE INDEX IF NOT EXISTS ix_hist_precio_variante_fecha
                ON historial_precio (variante_articulo_id, fecha)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_stock_articulo_variante
                ON stock (articulo_id, variante_id)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_articulo_modelo
                ON articulo (modelo)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_articulo_estado
                ON articulo (estado)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_var_estado
                ON variante_articulo (estado)
            """,
    };

    /** Solo PostgreSQL: no se pueden modelar con JPA. */
    private static final String[] POSTGRES_ONLY_INDEXES = {
            """
            CREATE INDEX IF NOT EXISTS ix_variante_articulo_activa
                ON variante_articulo (articulo_id)
                WHERE estado != 'INACTIVO'
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_articulo_codigo_trgm
                ON articulo USING gin (lower(codigo) gin_trgm_ops)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_articulo_modelo_trgm
                ON articulo USING gin (lower(modelo) gin_trgm_ops)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_variante_codigo_barras_trgm
                ON variante_articulo USING gin (lower(codigo_barras) gin_trgm_ops)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_marca_nombre_trgm
                ON marca USING gin (lower(nombre) gin_trgm_ops)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_categoria_nombre_trgm
                ON categoria USING gin (lower(nombre) gin_trgm_ops)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_genero_nombre_trgm
                ON genero USING gin (lower(nombre) gin_trgm_ops)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_talle_numero_trgm
                ON talle USING gin (lower(numero) gin_trgm_ops)
            """,
            """
            CREATE INDEX IF NOT EXISTS ix_color_nombre_trgm
                ON color USING gin (lower(nombre) gin_trgm_ops)
            """,
    };

    @Override
    public void run(ApplicationArguments args) {
        int aplicados = 0;

        for (String sql : ENTITY_BTREE_INDEXES) {
            if (executeIndex(sql)) {
                aplicados++;
            }
        }

        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        } catch (Exception e) {
            log.warn("No se pudo habilitar pg_trgm: {}", e.getMessage());
        }

        for (String sql : POSTGRES_ONLY_INDEXES) {
            if (executeIndex(sql)) {
                aplicados++;
            }
        }

        log.info("Índices catálogo artículos: {} aplicados (B-tree en entidades + trgm en migración).", aplicados);
    }

    private boolean executeIndex(String sql) {
        try {
            jdbcTemplate.execute(sql);
            return true;
        } catch (Exception e) {
            log.warn("Índice omitido: {}", e.getMessage());
            return false;
        }
    }
}
