package com.vida.apirest.repositories;

import com.vida.apirest.dto.dashboard.DashboardArticuloTopResponse;
import com.vida.apirest.dto.dashboard.DashboardClienteTopResponse;
import com.vida.apirest.dto.dashboard.DashboardCuotaPorEstadoResponse;
import com.vida.apirest.dto.dashboard.DashboardValorStockResponse;
import com.vida.apirest.dto.dashboard.DashboardVentaMetodoPagoResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DashboardQueryRepository {

    private static final String VENTAS_VALIDAS = "v.estado IN ('CONFIRMADA', 'ENTREGADA')";

    @PersistenceContext
    private EntityManager entityManager;

    public List<DashboardClienteTopResponse> topClientes(Long sucursalId, int limit) {
        String sql = """
                SELECT
                    c.id,
                    c.nombre,
                    c.apellido,
                    c.dni,
                    COALESCE(SUM(v.total), 0) AS total_pagado,
                    COUNT(v.id) AS cantidad_ventas
                FROM venta v
                INNER JOIN cliente c ON c.id = v.cliente_id
                """
                + whereVentasValidas(sucursalId, "v.sucursal_id")
                + """
                GROUP BY c.id, c.nombre, c.apellido, c.dni
                ORDER BY total_pagado DESC
                LIMIT :limit
                """;

        Query query = entityManager.createNativeQuery(sql);
        applySucursalParam(query, sucursalId);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<DashboardClienteTopResponse> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new DashboardClienteTopResponse(
                    toLong(row[0]),
                    toString(row[1]),
                    toString(row[2]),
                    toString(row[3]),
                    toBigDecimal(row[4]),
                    toLong(row[5])
            ));
        }
        return result;
    }

    public List<DashboardArticuloTopResponse> topArticulos(Long sucursalId, int limit) {
        String sql = """
                SELECT
                    a.id,
                    a.codigo,
                    m.nombre AS marca,
                    a.modelo,
                    COALESCE(SUM(vd.cantidad), 0) AS cantidad_vendida,
                    COALESCE(SUM(vd.total), 0) AS importe_total
                FROM venta_detalle vd
                INNER JOIN venta v ON v.id = vd.venta_id
                INNER JOIN articulo a ON a.id = vd.articulo_id
                LEFT JOIN marca m ON m.id = a.marca_id
                """
                + whereVentasValidas(sucursalId, "v.sucursal_id")
                + """
                GROUP BY a.id, a.codigo, m.nombre, a.modelo
                ORDER BY cantidad_vendida DESC, importe_total DESC
                LIMIT :limit
                """;

        Query query = entityManager.createNativeQuery(sql);
        applySucursalParam(query, sucursalId);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<DashboardArticuloTopResponse> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new DashboardArticuloTopResponse(
                    toLong(row[0]),
                    toString(row[1]),
                    toString(row[2]),
                    toString(row[3]),
                    toLong(row[4]),
                    toBigDecimal(row[5])
            ));
        }
        return result;
    }

    public List<DashboardVentaMetodoPagoResponse> ventasPorMetodoPago(Long sucursalId) {
        String sql = """
                SELECT
                    pv.metodo_pago,
                    COALESCE(SUM(pv.monto), 0) AS total,
                    COUNT(pv.id) AS cantidad_pagos
                FROM pago_venta pv
                INNER JOIN venta v ON v.id = pv.venta_id
                WHERE pv.estado = 'RECIBIDO'
                """
                + " AND " + VENTAS_VALIDAS
                + sucursalAndClause(sucursalId, "v.sucursal_id")
                + """
                GROUP BY pv.metodo_pago
                ORDER BY total DESC
                """;

        Query query = entityManager.createNativeQuery(sql);
        applySucursalParam(query, sucursalId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<DashboardVentaMetodoPagoResponse> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new DashboardVentaMetodoPagoResponse(
                    toString(row[0]),
                    toBigDecimal(row[1]),
                    toLong(row[2])
            ));
        }
        return result;
    }

    public DashboardValorStockResponse valorStock(Long sucursalId) {
        String sql = """
                SELECT
                    COALESCE(SUM(s.cantidad_disponible), 0) AS unidades,
                    COALESCE(SUM(
                        s.cantidad_disponible * COALESCE(hp.costo_nuevo, 0)
                    ), 0) AS valor_compra,
                    COALESCE(SUM(
                        s.cantidad_disponible * COALESCE(hp.precio_nuevo, lp.precio, 0)
                    ), 0) AS valor_venta
                FROM stock s
                LEFT JOIN variante_articulo va ON va.id = s.variante_id
                LEFT JOIN lista_precio lp ON lp.id = va.lista_precio_id
                LEFT JOIN LATERAL (
                    SELECT hp2.precio_nuevo, hp2.costo_nuevo
                    FROM historial_precio hp2
                    WHERE hp2.variante_articulo_id = s.variante_id
                    ORDER BY hp2.fecha DESC
                    LIMIT 1
                ) hp ON TRUE
                WHERE s.cantidad_disponible > 0
                """
                + sucursalAndClause(sucursalId, "s.sucursal_id");

        Query query = entityManager.createNativeQuery(sql);
        applySucursalParam(query, sucursalId);

        Object[] row = (Object[]) query.getSingleResult();
        return new DashboardValorStockResponse(
                toLong(row[0]),
                toBigDecimal(row[1]),
                toBigDecimal(row[2])
        );
    }

    public List<DashboardCuotaPorEstadoResponse> resumenCuotasPorEstado() {
        String sql = """
                SELECT
                    q.estado,
                    COUNT(*) AS cantidad,
                    COALESCE(SUM(
                        CASE q.estado
                            WHEN 'PAGADA' THEN COALESCE(q.monto, 0)
                            WHEN 'CANCELADA' THEN COALESCE(q.monto, 0)
                            WHEN 'ELIMINADA' THEN COALESCE(q.monto, 0)
                            WHEN 'VENCIDA' THEN COALESCE(q.saldo, 0)
                                + ROUND(COALESCE(q.monto, 0) * 0.10, 2)
                            WHEN 'PENDIENTE' THEN COALESCE(q.saldo, q.monto, 0)
                            ELSE 0
                        END
                    ), 0) AS total
                FROM cuota q
                GROUP BY q.estado
                ORDER BY q.estado
                """;

        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<DashboardCuotaPorEstadoResponse> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new DashboardCuotaPorEstadoResponse(
                    toString(row[0]),
                    toLong(row[1]),
                    toBigDecimal(row[2])
            ));
        }
        return result;
    }

    private static String whereVentasValidas(Long sucursalId, String sucursalColumn) {
        return " WHERE " + VENTAS_VALIDAS + sucursalAndClause(sucursalId, sucursalColumn);
    }

    private static String sucursalAndClause(Long sucursalId, String column) {
        if (sucursalId == null) {
            return "\n";
        }
        return " AND " + column + " = :sucursalId\n";
    }

    private static void applySucursalParam(Query query, Long sucursalId) {
        if (sucursalId != null) {
            query.setParameter("sucursalId", sucursalId);
        }
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private static String toString(Object value) {
        return value == null ? "" : value.toString();
    }
}
