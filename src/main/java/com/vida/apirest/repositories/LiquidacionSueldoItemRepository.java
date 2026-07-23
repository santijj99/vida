package com.vida.apirest.repositories;

import com.vida.apirest.model.sueldo.LiquidacionSueldoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface LiquidacionSueldoItemRepository extends JpaRepository<LiquidacionSueldoItem, Long> {

    List<LiquidacionSueldoItem> findByLiquidacion_IdAndIdIn(Long liquidacionId, List<Long> ids);

    @Query(value = """
            SELECT COALESCE(SUM(v.total), 0)
            FROM venta v
            WHERE v.empleado_id = :empleadoId
              AND v.estado IN ('CONFIRMADA', 'ENTREGADA')
              AND v.fecha_venta >= :desde
              AND v.fecha_venta < :hasta
              AND (:sucursalId IS NULL OR v.sucursal_id = :sucursalId)
            """, nativeQuery = true)
    BigDecimal sumVentasEmpleado(
            @Param("empleadoId") Long empleadoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("sucursalId") Long sucursalId
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM venta v
            WHERE v.empleado_id = :empleadoId
              AND v.estado IN ('CONFIRMADA', 'ENTREGADA')
              AND v.fecha_venta >= :desde
              AND v.fecha_venta < :hasta
              AND (:sucursalId IS NULL OR v.sucursal_id = :sucursalId)
            """, nativeQuery = true)
    long countVentasEmpleado(
            @Param("empleadoId") Long empleadoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("sucursalId") Long sucursalId
    );

    @Query(value = """
            SELECT COALESCE(SUM(d.cantidad), 0)
            FROM venta_detalle d
            JOIN venta v ON v.id = d.venta_id
            WHERE v.empleado_id = :empleadoId
              AND v.estado IN ('CONFIRMADA', 'ENTREGADA')
              AND v.fecha_venta >= :desde
              AND v.fecha_venta < :hasta
              AND (:sucursalId IS NULL OR v.sucursal_id = :sucursalId)
            """, nativeQuery = true)
    long sumUnidadesEmpleado(
            @Param("empleadoId") Long empleadoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("sucursalId") Long sucursalId
    );
}
