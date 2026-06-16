package com.vida.apirest.repositories;

import com.vida.apirest.dto.almacen.StockDepositoResponse;
import com.vida.apirest.dto.common.PageResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TransferenciaStockQueryRepository {

    private static final int DEFAULT_PAGE_SIZE = 15;
    private static final int MAX_PAGE_SIZE = 100;

    @PersistenceContext
    private EntityManager entityManager;

    public PageResponse<StockDepositoResponse> findStockByDepositoPage(
            Long depositoId,
            String q,
            int page,
            int size
    ) {
        int safeSize = normalizeSize(size);
        int safePage = Math.max(page, 0);
        String filtro = buildFiltro(q);

        long total = countStock(depositoId, q);
        if (total == 0) {
            return PageResponse.of(List.of(), safePage, safeSize, 0);
        }

        String sql = """
                SELECT
                    s.id AS stock_id,
                    a.id AS articulo_id,
                    v.id AS variante_id,
                    a.codigo,
                    m.nombre AS marca,
                    a.modelo,
                    tal.numero AS talle,
                    col.nombre AS color,
                    v.codigo_barras,
                    s.cantidad_disponible AS stock
                FROM stock s
                INNER JOIN variante_articulo v ON v.id = s.variante_id
                INNER JOIN articulo a ON a.id = s.articulo_id
                LEFT JOIN marca m ON m.id = a.marca_id
                LEFT JOIN talle tal ON tal.id = v.talle_id
                LEFT JOIN color col ON col.id = v.color_id
                WHERE s.deposito_id = :depositoId
                  AND s.variante_id IS NOT NULL
                  AND s.cantidad_disponible > 0
                """ + filtro + """
                ORDER BY a.codigo ASC, v.id ASC
                LIMIT :limit OFFSET :offset
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("depositoId", depositoId);
        applyFiltroParams(query, q);
        query.setParameter("limit", safeSize);
        query.setParameter("offset", (long) safePage * safeSize);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return PageResponse.of(mapRows(rows), safePage, safeSize, total);
    }

    private long countStock(Long depositoId, String q) {
        String filtro = buildFiltro(q);
        String sql = """
                SELECT COUNT(*)
                FROM stock s
                INNER JOIN variante_articulo v ON v.id = s.variante_id
                INNER JOIN articulo a ON a.id = s.articulo_id
                LEFT JOIN marca m ON m.id = a.marca_id
                LEFT JOIN talle tal ON tal.id = v.talle_id
                LEFT JOIN color col ON col.id = v.color_id
                WHERE s.deposito_id = :depositoId
                  AND s.variante_id IS NOT NULL
                  AND s.cantidad_disponible > 0
                """ + filtro;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("depositoId", depositoId);
        applyFiltroParams(query, q);
        return ((Number) query.getSingleResult()).longValue();
    }

    private String buildFiltro(String q) {
        if (q == null || q.isBlank()) {
            return "";
        }
        return """
                 AND (
                    LOWER(a.codigo) LIKE LOWER(:q)
                    OR LOWER(COALESCE(m.nombre, '')) LIKE LOWER(:q)
                    OR LOWER(COALESCE(a.modelo, '')) LIKE LOWER(:q)
                    OR LOWER(COALESCE(v.codigo_barras, '')) LIKE LOWER(:q)
                    OR LOWER(COALESCE(tal.numero, '')) LIKE LOWER(:q)
                    OR LOWER(COALESCE(col.nombre, '')) LIKE LOWER(:q)
                 )
                """;
    }

    private void applyFiltroParams(Query query, String q) {
        if (q != null && !q.isBlank()) {
            query.setParameter("q", "%" + q.trim() + "%");
        }
    }

    private List<StockDepositoResponse> mapRows(List<Object[]> rows) {
        List<StockDepositoResponse> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            result.add(new StockDepositoResponse(
                    toLong(r[0]),
                    toLong(r[1]),
                    toLong(r[2]),
                    toString(r[3]),
                    toString(r[4]),
                    toString(r[5]),
                    toString(r[6]),
                    toString(r[7]),
                    toString(r[8]),
                    toInteger(r[9])
            ));
        }
        return result;
    }

    private int normalizeSize(int size) {
        if (size <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Long toLong(Object v) {
        return v == null ? null : ((Number) v).longValue();
    }

    private Integer toInteger(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    private String toString(Object o) {
        return o == null ? "" : o.toString();
    }
}
