package com.vida.apirest.repositories;

import com.vida.apirest.dto.ariticulo.ArticuloParaVentaResponse;
import com.vida.apirest.dto.ariticulo.ArticuloTablaRowResponse;
import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.utils.PaginationUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
public class ArticuloTablaQueryRepository {

    /** Precio vigente por variante (1 fila por variante, usa ix_hist_precio_variante_fecha). */
    private static final String JOIN_PRECIO_ACTUAL = """
            LEFT JOIN (
                SELECT DISTINCT ON (hp.variante_articulo_id)
                    hp.variante_articulo_id,
                    hp.precio_nuevo
                FROM historial_precio hp
                ORDER BY hp.variante_articulo_id, hp.fecha DESC
            ) hp ON hp.variante_articulo_id = v.id
            """;

    /** Subcategoría única por artículo (no incluye clasificaciones). */
    private static final String JOIN_SUBCATEGORIA = """
            LEFT JOIN (
                SELECT DISTINCT ON (ta.articulo_id)
                    ta.articulo_id,
                    t.nombre
                FROM taxon_articulo ta
                JOIN taxon t ON t.id = ta.taxon_id
                WHERE ta.tipo = 'SUBCATEGORIA'
                ORDER BY ta.articulo_id, t.id
            ) sub ON sub.articulo_id = a.id
            """;

    /** Stock total agregado por variante (evita LATERAL por fila). */
    private static final String JOIN_STOCK_TOTAL = """
            LEFT JOIN (
                SELECT s.articulo_id, s.variante_id,
                       COALESCE(SUM(s.cantidad_disponible), 0) AS cantidad
                FROM stock s
                WHERE s.variante_id IS NOT NULL
                GROUP BY s.articulo_id, s.variante_id
            ) stock_tot ON stock_tot.articulo_id = a.id AND stock_tot.variante_id = v.id
            """;

    private static final String JOIN_VENTA_BUSQUEDA = """
            LEFT JOIN marca m ON m.id = a.marca_id
            LEFT JOIN categoria cat ON cat.id = a.categoria_id
            LEFT JOIN genero g ON g.id = a.genero_id
            LEFT JOIN talle tal ON tal.id = v.talle_id
            LEFT JOIN color col ON col.id = v.color_id
            """;

    private static final String JOIN_TABLA_DIMENSIONES = """
            LEFT JOIN marca m ON m.id = a.marca_id
            LEFT JOIN categoria cat ON cat.id = a.categoria_id
            LEFT JOIN genero g ON g.id = a.genero_id
            LEFT JOIN talle tal ON tal.id = v.talle_id
            LEFT JOIN color col ON col.id = v.color_id
            """;

    @PersistenceContext
    private EntityManager entityManager;

    public PageResponse<ArticuloTablaRowResponse> findTablaPage(
            String categoria,
            String subCategoria,
            List<String> clasificaciones,
            String genero,
            String marca,
            String talle,
            String color,
            String q,
            Long depositoId,
            int page,
            int size
    ) {
        int safeSize = PaginationUtils.normalizeSize(size);
        int safePage = PaginationUtils.normalizePage(page);
        FilterSql filters = buildTablaFilters(
                categoria, subCategoria, clasificaciones, genero, marca, talle, color, q, depositoId);

        long total = countTabla(filters, depositoId);
        if (total == 0) {
            return PageResponse.of(List.of(), safePage, safeSize, 0);
        }

        String stockJoin = depositoId != null
                ? """
                INNER JOIN stock s_dep ON s_dep.deposito_id = :depositoId
                    AND s_dep.variante_id = v.id
                    AND s_dep.articulo_id = a.id
                """
                : "";

        String cantidadExpr = depositoId != null
                ? "COALESCE(s_dep.cantidad_disponible, 0)"
                : "stock_tot.cantidad";

        String stockAggregateJoin = depositoId != null ? "" : JOIN_STOCK_TOTAL;

        String sql = """
                SELECT
                    a.id AS articulo_id,
                    v.id AS variante_id,
                    a.codigo,
                    m.nombre AS marca,
                    a.modelo,
                    cat.nombre AS categoria,
                    sub.nombre AS sub_categoria,
                    g.nombre AS genero,
                    tal.numero AS talle,
                    col.nombre AS color,
                    v.codigo_barras,
                    COALESCE(hp.precio_nuevo, lp.precio) AS precio,
                    """
                + cantidadExpr
                + """
                 AS cantidad
                FROM variante_articulo v
                INNER JOIN articulo a ON a.id = v.articulo_id
                """
                + stockJoin
                + JOIN_TABLA_DIMENSIONES
                + """
                LEFT JOIN lista_precio lp ON lp.id = v.lista_precio_id
                """
                + JOIN_PRECIO_ACTUAL
                + JOIN_SUBCATEGORIA
                + stockAggregateJoin
                + """
                WHERE v.estado != 'INACTIVO'
                  AND a.estado != 'ARCHIVADO'
                """
                + filters.whereClause()
                + """
                ORDER BY a.codigo ASC, v.id ASC
                LIMIT :limit OFFSET :offset
                """;

        Query query = entityManager.createNativeQuery(sql);
        filters.applyParams(query);
        query.setParameter("limit", safeSize);
        query.setParameter("offset", (long) safePage * safeSize);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return PageResponse.of(mapTablaRows(rows), safePage, safeSize, total);
    }

    public PageResponse<ArticuloParaVentaResponse> findParaVentaPage(
            Long sucursalId,
            String categoria,
            String subCategoria,
            List<String> clasificaciones,
            String genero,
            String marca,
            String talle,
            String color,
            String q,
            int page,
            int size
    ) {
        int safeSize = PaginationUtils.normalizeSize(size);
        int safePage = PaginationUtils.normalizePage(page);
        FilterSql filters = buildVentaFilters(
                sucursalId, categoria, subCategoria, clasificaciones, genero, marca, talle, color, q);

        long total = countVenta(filters, tieneBusqueda(q));
        if (total == 0) {
            return PageResponse.of(List.of(), safePage, safeSize, 0);
        }

        String sql = """
                SELECT
                    a.id AS articulo_id,
                    v.id AS variante_id,
                    a.codigo,
                    m.nombre AS marca,
                    a.modelo,
                    tal.numero AS talle,
                    col.nombre AS color,
                    v.codigo_barras,
                    COALESCE(hp.precio_nuevo, lp.precio) AS precio,
                    s.cantidad_disponible AS stock
                FROM stock s
                INNER JOIN variante_articulo v ON v.id = s.variante_id
                INNER JOIN articulo a ON a.id = s.articulo_id
                """
                + JOIN_VENTA_BUSQUEDA
                + """
                LEFT JOIN lista_precio lp ON lp.id = v.lista_precio_id
                """
                + JOIN_PRECIO_ACTUAL
                + """
                WHERE s.sucursal_id = :sucursalId
                  AND s.variante_id IS NOT NULL
                  AND s.cantidad_disponible > 0
                  AND v.estado != 'INACTIVO'
                  AND a.estado != 'ARCHIVADO'
                """
                + filters.whereClause()
                + """
                AND COALESCE(hp.precio_nuevo, lp.precio) > 0
                ORDER BY a.codigo ASC, v.id ASC
                LIMIT :limit OFFSET :offset
                """;

        Query query = entityManager.createNativeQuery(sql);
        filters.applyParams(query);
        query.setParameter("limit", safeSize);
        query.setParameter("offset", (long) safePage * safeSize);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return PageResponse.of(mapVentaRows(rows), safePage, safeSize, total);
    }

    private long countTabla(FilterSql filters, Long depositoId) {
        String stockJoin = depositoId != null
                ? """
                INNER JOIN stock s_dep ON s_dep.deposito_id = :depositoId
                    AND s_dep.variante_id = v.id
                    AND s_dep.articulo_id = a.id
                """
                : "";

        String sql = """
                SELECT COUNT(*)
                FROM variante_articulo v
                INNER JOIN articulo a ON a.id = v.articulo_id
                """
                + stockJoin
                + buildTablaDimensionJoins(filters)
                + """
                WHERE v.estado != 'INACTIVO'
                  AND a.estado != 'ARCHIVADO'
                """
                + filters.whereClause();

        Query query = entityManager.createNativeQuery(sql);
        filters.applyParams(query);
        return ((Number) query.getSingleResult()).longValue();
    }

    private long countVenta(FilterSql filters, boolean conBusqueda) {
        String busquedaJoins = JOIN_VENTA_BUSQUEDA;

        String sql = """
                SELECT COUNT(*)
                FROM stock s
                INNER JOIN variante_articulo v ON v.id = s.variante_id
                INNER JOIN articulo a ON a.id = s.articulo_id
                """
                + busquedaJoins
                + """
                LEFT JOIN lista_precio lp ON lp.id = v.lista_precio_id
                """
                + JOIN_PRECIO_ACTUAL
                + """
                WHERE s.sucursal_id = :sucursalId
                  AND s.variante_id IS NOT NULL
                  AND s.cantidad_disponible > 0
                  AND v.estado != 'INACTIVO'
                  AND a.estado != 'ARCHIVADO'
                """
                + filters.whereClause()
                + """
                AND COALESCE(hp.precio_nuevo, lp.precio) > 0
                """;

        Query query = entityManager.createNativeQuery(sql);
        filters.applyParams(query);
        return ((Number) query.getSingleResult()).longValue();
    }

    private static String buildTablaDimensionJoins(FilterSql filters) {
        if (!necesitaJoinsDimension(filters)) {
            return "";
        }
        return """
                LEFT JOIN marca m ON m.id = a.marca_id
                LEFT JOIN categoria cat ON cat.id = a.categoria_id
                LEFT JOIN genero g ON g.id = a.genero_id
                LEFT JOIN talle tal ON tal.id = v.talle_id
                LEFT JOIN color col ON col.id = v.color_id
                """;
    }

    private FilterSql buildTablaFilters(
            String categoria, String subCategoria, List<String> clasificaciones,
            String genero, String marca, String talle, String color,
            String q, Long depositoId) {
        StringBuilder where = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        if (depositoId != null) {
            params.put("depositoId", depositoId);
        }

        appendFiltrosDimensiones(where, params, categoria, subCategoria, clasificaciones, genero, marca, talle, color);

        appendSearchFilter(where, params, q,
                "a.codigo", "a.modelo", "m.nombre", "cat.nombre", "g.nombre",
                "tal.numero", "col.nombre", "v.codigo_barras");

        return new FilterSql(where.toString(), params);
    }

    private FilterSql buildVentaFilters(
            Long sucursalId,
            String categoria,
            String subCategoria,
            List<String> clasificaciones,
            String genero,
            String marca,
            String talle,
            String color,
            String q) {
        StringBuilder where = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        params.put("sucursalId", sucursalId);

        appendFiltrosDimensiones(where, params, categoria, subCategoria, clasificaciones, genero, marca, talle, color);

        appendSearchFilter(where, params, q,
                "a.codigo", "a.modelo", "m.nombre", "cat.nombre", "g.nombre",
                "tal.numero", "col.nombre", "v.codigo_barras");

        return new FilterSql(where.toString(), params);
    }

    private void appendFiltrosDimensiones(
            StringBuilder where,
            Map<String, Object> params,
            String categoria,
            String subCategoria,
            List<String> clasificaciones,
            String genero,
            String marca,
            String talle,
            String color) {
        appendExactFilter(where, params, "cat.nombre", "categoria", categoria);
        appendExactFilter(where, params, "g.nombre", "genero", genero);
        appendExactFilter(where, params, "m.nombre", "marca", marca);
        appendExactFilter(where, params, "tal.numero", "talle", talle);
        appendExactFilter(where, params, "col.nombre", "color", color);

        if (subCategoria != null && !subCategoria.isBlank()) {
            where.append("""
                     AND EXISTS (
                        SELECT 1 FROM taxon_articulo ta
                        JOIN taxon t ON t.id = ta.taxon_id
                        WHERE ta.articulo_id = a.id
                          AND ta.tipo = 'SUBCATEGORIA'
                          AND LOWER(t.nombre) = LOWER(:subCategoriaFiltro)
                     )
                    """);
            params.put("subCategoriaFiltro", subCategoria.trim());
        }

        if (clasificaciones != null && !clasificaciones.isEmpty()) {
            List<String> nombres = clasificaciones.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toLowerCase)
                    .distinct()
                    .toList();
            if (!nombres.isEmpty()) {
                where.append("""
                         AND EXISTS (
                            SELECT 1 FROM taxon_articulo ta
                            JOIN taxon t ON t.id = ta.taxon_id
                            WHERE ta.articulo_id = a.id
                              AND ta.tipo = 'CLASIFICACION'
                              AND LOWER(t.nombre) IN (:clasificacionesFiltro)
                         )
                        """);
                params.put("clasificacionesFiltro", nombres);
            }
        }
    }

    private void appendExactFilter(
            StringBuilder where, Map<String, Object> params,
            String column, String param, String value) {
        if (value != null && !value.isBlank()) {
            where.append(" AND LOWER(").append(column).append(") = LOWER(:").append(param).append(")");
            params.put(param, value.trim());
        }
    }

    private void appendSearchFilter(
            StringBuilder where, Map<String, Object> params, String q, String... columns) {
        if (q == null || q.isBlank()) {
            return;
        }

        String trimmed = q.trim();
        String[] tokens = trimmed.split("\\s+");
        boolean incluyeMarcaModelo = contiene(columns, "m.nombre") && contiene(columns, "a.modelo");

        if (tokens.length == 1) {
            where.append(" AND (");
            appendCoincidenciaEnColumnas(where, columns, "q");
            if (incluyeMarcaModelo) {
                appendCoincidenciaMarcaModeloConcatenado(where, "q");
            }
            where.append(")");
            params.put("q", "%" + trimmed + "%");
            return;
        }

        params.put("qFull", "%" + trimmed + "%");

        if (incluyeMarcaModelo && tokens.length == 2) {
            where.append("""
                     AND (
                        (COALESCE(m.nombre, '') ILIKE :q0 AND COALESCE(a.modelo, '') ILIKE :q1)
                        OR (COALESCE(m.nombre, '') ILIKE :q1 AND COALESCE(a.modelo, '') ILIKE :q0)
                        OR (COALESCE(m.nombre, '') || ' ' || COALESCE(a.modelo, '')) ILIKE :qFull
                        OR (COALESCE(a.modelo, '') || ' ' || COALESCE(m.nombre, '')) ILIKE :qFull
                     )
                    """);
            params.put("q0", "%" + tokens[0] + "%");
            params.put("q1", "%" + tokens[1] + "%");
            return;
        }

        for (int i = 0; i < tokens.length; i++) {
            String param = "q" + i;
            where.append(" AND (");
            appendCoincidenciaEnColumnas(where, columns, param);
            if (incluyeMarcaModelo) {
                appendCoincidenciaMarcaModeloConcatenado(where, param);
            }
            where.append(")");
            params.put(param, "%" + tokens[i] + "%");
        }
    }

    private static boolean necesitaJoinsDimension(FilterSql filters) {
        if (filters.params().containsKey("categoria")
                || filters.params().containsKey("genero")
                || filters.params().containsKey("marca")
                || filters.params().containsKey("talle")
                || filters.params().containsKey("color")) {
            return true;
        }
        return filters.params().keySet().stream()
                .anyMatch(k -> k.equals("q") || k.startsWith("q"));
    }

    private static boolean tieneBusqueda(String q) {
        return q != null && !q.isBlank();
    }

    private static boolean contiene(String[] columns, String column) {
        for (String c : columns) {
            if (c.equals(column)) {
                return true;
            }
        }
        return false;
    }

    private static void appendCoincidenciaEnColumnas(
            StringBuilder where, String[] columns, String paramName) {
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                where.append(" OR ");
            }
            where.append("COALESCE(").append(columns[i]).append(", '') ILIKE :").append(paramName);
        }
    }

    private static void appendCoincidenciaMarcaModeloConcatenado(StringBuilder where, String paramName) {
        where.append(" OR (COALESCE(m.nombre, '') || ' ' || COALESCE(a.modelo, '')) ILIKE :")
                .append(paramName);
        where.append(" OR (COALESCE(a.modelo, '') || ' ' || COALESCE(m.nombre, '')) ILIKE :")
                .append(paramName);
    }

    private List<ArticuloTablaRowResponse> mapTablaRows(List<Object[]> rows) {
        List<ArticuloTablaRowResponse> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            result.add(new ArticuloTablaRowResponse(
                    toLong(r[0]),
                    toLong(r[1]),
                    toString(r[2]),
                    toString(r[3]),
                    toString(r[4]),
                    toString(r[5]),
                    toString(r[6]),
                    toString(r[7]),
                    toString(r[8]),
                    toString(r[9]),
                    toString(r[10]),
                    toBigDecimal(r[11]),
                    toInteger(r[12])
            ));
        }
        return result;
    }

    private List<ArticuloParaVentaResponse> mapVentaRows(List<Object[]> rows) {
        List<ArticuloParaVentaResponse> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            result.add(new ArticuloParaVentaResponse(
                    toLong(r[0]),
                    toLong(r[1]),
                    toString(r[2]),
                    toString(r[3]),
                    toString(r[4]),
                    toString(r[5]),
                    toString(r[6]),
                    toString(r[7]),
                    toBigDecimal(r[8]),
                    null,
                    false,
                    toInteger(r[9])
            ));
        }
        return result;
    }

    private Long toLong(Object v) {
        return v == null ? null : ((Number) v).longValue();
    }

    private Integer toInteger(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    private String toString(Object o) {
        return o == null ? null : o.toString();
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(o.toString());
    }

    private record FilterSql(String whereClause, Map<String, Object> params) {
        void applyParams(Query query) {
            params.forEach(query::setParameter);
        }
    }
}
