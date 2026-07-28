package com.vida.apirest.repositories;

import com.vida.apirest.model.sueldo.LiquidacionSueldo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LiquidacionSueldoRepository extends JpaRepository<LiquidacionSueldo, Long> {

    @EntityGraph(attributePaths = {"sucursal", "items", "items.empleado", "items.cuentaPago", "items.movimiento"})
    @Query("SELECT l FROM LiquidacionSueldo l WHERE l.id = :id")
    Optional<LiquidacionSueldo> findByIdWithItems(@Param("id") Long id);

    /** Bloqueo pesimista de la cabecera para evitar doble pago concurrente. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LiquidacionSueldo l WHERE l.id = :id")
    Optional<LiquidacionSueldo> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "sucursal",
            "items",
            "items.empleado",
            "items.cuentaPago",
            "items.movimiento"
    })
    @Query("""
            SELECT DISTINCT l FROM LiquidacionSueldo l
            WHERE (:sucursalId IS NULL OR l.sucursal.id = :sucursalId)
            ORDER BY l.createdAt DESC
            """)
    List<LiquidacionSueldo> listar(@Param("sucursalId") Long sucursalId);

    /**
     * Empleados que ya figuran en una liquidación activa (cualquier sucursal)
     * cuyo rango de fechas se solapa con [desde, hasta].
     * El control es global por empleado para evitar pagar el sueldo fijo dos veces
     * en sucursales distintas el mismo período.
     */
    @Query("""
            SELECT DISTINCT i.empleado.id
            FROM LiquidacionSueldoItem i
            JOIN i.liquidacion l
            WHERE l.estado <> :cancelada
              AND l.fechaDesde <= :hasta
              AND l.fechaHasta >= :desde
              AND i.empleado.id IN :empleadoIds
            """)
    List<Long> findEmpleadoIdsSolapados(
            @Param("desde") java.time.LocalDate desde,
            @Param("hasta") java.time.LocalDate hasta,
            @Param("empleadoIds") List<Long> empleadoIds,
            @Param("cancelada") LiquidacionSueldo.EstadoLiquidacion cancelada
    );
}
