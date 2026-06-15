package com.vida.apirest.repositories;

import com.vida.apirest.dto.ariticulo.ArticuloParaVentaResponse;
import com.vida.apirest.dto.ariticulo.ArticuloTablaRowResponse;
import com.vida.apirest.dto.common.PageResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ArticuloTablaQueryRepository {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 15;

    @PersistenceContext
    private EntityManager entityManager;

    public PageResponse<ArticuloTablaRowResponse> findTablaPage(
            String categoria,
            String subCategoria,
            String genero,
            String marca,
            String q,
            int page,
            int size
    ) {
        int safeSize = normalizeSize(size);
        int safePage = Math.max(page, 0);
        FilterSql filters = buildTablaFilters(categoria, subCategoria, genero, marca, q);

        long total = countTabla(filters);
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
                    cat.nombre AS categoria,
                    (SELECT t.nombre FROM taxon_articulo ta
                     JOIN taxon t ON t.id = ta.taxon_id
                     WHERE ta.articulo_id = a.id
                     ORDER BY t.id LIMIT 1) AS sub_categoria,
                    g.nombre AS genero,
                    tal.numero AS talle,
                    col.nombre AS color,
                    v.codigo_barras,
                    COALESCE(
                        (SELECT hp.precio_nuevo FROM historial_precio hp
                         WHERE hp.variante_articulo_id = v.id
                         ORDER BY hp.fecha DESC LIMIT 1),
                        lp.precio
                    ) AS precio,
                    COALESCE((
                        SELECT SUM(s.cantidad_disponible) FROM stock s
                        WHERE s.articulo_id = a.id AND s.variante_id = v.id
                    ), 0) AS cantidad
                FROM variante_articulo v
                INNER JOIN articulo a ON a.id = v.articulo_id
                LEFT JOIN marca m ON m.id = a.marca_id
                LEFT JOIN categoria cat ON cat.id = a.categoria_id
                LEFT JOIN genero g ON g.id = a.genero_id
                LEFT JOIN talle tal ON tal.id = v.talle_id
                LEFT JOIN color col ON col.id = v.color_id
                LEFT JOIN lista_precio lp ON lp.id = v.lista_precio_id
                WHERE 1 = 1
                """ + filters.whereClause() + """
                ORDER BY a.codigo ASC, v.id ASC
                LIMIT :limit OFFSET :offset
                """;

        Query query = entityManager.createNativeQuery(sql);
        filters.applyParams(query);
        query.setParameter("limit", safeSize);
        query.setParameter("offset", (long) safePage * safeSize);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<ArticuloTablaRowResponse> content = mapTablaRows(rows);
        return PageResponse.of(content, safePage, safeSize, total);
    }

    public PageResponse<ArticuloParaVentaResponse> findParaVentaPage(
            Long sucursalId,
            String q,
            int page,
            int size
    ) {
        int safeSize = normalizeSize(size);
        int safePage = Math.max(page, 0);
        FilterSql filters = buildVentaFilters(sucursalId, q);

        long total = countVenta(filters);
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
                    COALESCE(
                        (SELECT hp.precio_nuevo FROM historial_precio hp
                         WHERE hp.variante_articulo_id = v.id
                         ORDER BY hp.fecha DESC LIMIT 1),
                        lp.precio
                    ) AS precio,
                    s.cantidad_disponible AS stock
                FROM stock s
                INNER JOIN variante_articulo v ON v.id = s.variante_id
                INNER JOIN articulo a ON a.id = s.articulo_id
                LEFT JOIN marca m ON m.id = a.marca_id
                LEFT JOIN talle tal ON tal.id = v.talle_id
                LEFT JOIN color col ON col.id = v.color_id
                LEFT JOIN lista_precio lp ON lp.id = v.lista_precio_id
                WHERE s.sucursal_id = :sucursalId
                  AND s.variante_id IS NOT NULL
                  AND s.cantidad_disponible > 0
                """ + filters.whereClause() + """
                AND COALESCE(
                    (SELECT hp.precio_nuevo FROM historial_precio hp
                     WHERE hp.variante_articulo_id = v.id
                     ORDER BY hp.fecha DESC LIMIT 1),
                    lp.precio
                ) > 0
                ORDER BY a.codigo ASC, v.id ASC
                LIMIT :limit OFFSET :offset
                """;

        Query query = entityManager.createNativeQuery(sql);
        filters.applyParams(query);
        query.setParameter("limit", safeSize);
        query.setParameter("offset", (long) safePage * safeSize);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<ArticuloParaVentaResponse> content = mapVentaRows(rows);
        return PageResponse.of(content, safePage, safeSize, total);
    }

    private long countTabla(FilterSql filters) {
        String sql = """
                SELECT COUNT(*)
                FROM variante_articulo v
                INNER JOIN articulo a ON a.id = v.articulo_id
                LEFT JOIN marca m ON m.id = a.marca_id
                LEFT JOIN categoria cat ON cat.id = a.categoria_id
                LEFT JOIN genero g ON g.id = a.genero_id
                LEFT JOIN talle tal ON tal.id = v.talle_id
                LEFT JOIN color col ON col.id = v.color_id
                WHERE 1 = 1
                """ + filters.whereClause();
        Query query = entityManager.createNativeQuery(sql);
        filters.applyParams(query);
        return ((Number) query.getSingleResult()).longValue();
    }

    private long countVenta(FilterSql filters) {
        String sql = """
                SELECT COUNT(*)
                FROM stock s
                INNER JOIN variante_articulo v ON v.id = s.variante_id
                INNER JOIN articulo a ON a.id = s.articulo_id
                LEFT JOIN marca m ON m.id = a.marca_id
                LEFT JOIN talle tal ON tal.id = v.talle_id
                LEFT JOIN color col ON col.id = v.color_id
                LEFT JOIN lista_precio lp ON lp.id = v.lista_precio_id
                WHERE s.sucursal_id = :sucursalId
                  AND s.variante_id IS NOT NULL
                  AND s.cantidad_disponible > 0
                """ + filters.whereClause() + """
                AND COALESCE(
                    (SELECT hp.precio_nuevo FROM historial_precio hp
                     WHERE hp.variante_articulo_id = v.id
                     ORDER BY hp.fecha DESC LIMIT 1),
                    lp.precio
                ) > 0
                """;
        Query query = entityManager.createNativeQuery(sql);
        filters.applyParams(query);
        return ((Number) query.getSingleResult()).longValue();
    }

    private FilterSql buildTablaFilters(
            String categoria, String subCategoria, String genero, String marca, String q) {
        StringBuilder where = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        where.append(" AND a.estado != 'ARCHIVADO'");
        where.append(" AND v.estado != 'INACTIVO'");

        appendExactFilter(where, params, "cat.nombre", "categoria", categoria);
        appendExactFilter(where, params, "g.nombre", "genero", genero);
        appendExactFilter(where, params, "m.nombre", "marca", marca);

        if (subCategoria != null && !subCategoria.isBlank()) {
            where.append("""
                     AND EXISTS (
                        SELECT 1 FROM taxon_articulo ta
                        JOIN taxon t ON t.id = ta.taxon_id
                        WHERE ta.articulo_id = a.id
                          AND LOWER(t.nombre) = LOWER(:subCategoria)
                     )
                    """);
            params.put("subCategoria", subCategoria.trim());
        }

        appendSearchFilter(where, params, q,
                "a.codigo", "a.modelo", "m.nombre", "cat.nombre", "g.nombre",
                "tal.numero", "col.nombre", "v.codigo_barras");

        return new FilterSql(where.toString(), params);
    }

    private FilterSql buildVentaFilters(Long sucursalId, String q) {
        StringBuilder where = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        where.append(" AND a.estado != 'ARCHIVADO'");
        where.append(" AND v.estado != 'INACTIVO'");
        
        params.put("sucursalId", sucursalId);
        appendSearchFilter(where, params, q,
                "a.codigo", "a.modelo", "m.nombre", "tal.numero", "col.nombre", "v.codigo_barras");
        return new FilterSql(where.toString(), params);
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
        where.append(" AND (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                where.append(" OR ");
            }
            where.append("LOWER(COALESCE(").append(columns[i]).append(", '')) LIKE LOWER(:q)");
        }
        where.append(")");
        params.put("q", "%" + q.trim() + "%");
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

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
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