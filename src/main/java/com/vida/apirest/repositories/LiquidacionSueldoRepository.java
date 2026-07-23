package com.vida.apirest.repositories;

import com.vida.apirest.model.sueldo.LiquidacionSueldo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LiquidacionSueldoRepository extends JpaRepository<LiquidacionSueldo, Long> {

    @EntityGraph(attributePaths = {"sucursal", "items", "items.empleado", "items.cuentaPago"})
    @Query("SELECT l FROM LiquidacionSueldo l WHERE l.id = :id")
    Optional<LiquidacionSueldo> findByIdWithItems(@Param("id") Long id);

    @EntityGraph(attributePaths = {"sucursal"})
    @Query("""
            SELECT l FROM LiquidacionSueldo l
            WHERE (:sucursalId IS NULL OR l.sucursal.id = :sucursalId)
            ORDER BY l.createdAt DESC
            """)
    List<LiquidacionSueldo> listar(@Param("sucursalId") Long sucursalId);

    @Query("""
            SELECT COUNT(l) FROM LiquidacionSueldo l
            WHERE l.estado <> :cancelada
              AND l.fechaDesde <= :hasta
              AND l.fechaHasta >= :desde
              AND (:sucursalId IS NULL OR l.sucursal IS NULL OR l.sucursal.id = :sucursalId)
            """)
    long countSolapadas(
            @Param("desde") java.time.LocalDate desde,
            @Param("hasta") java.time.LocalDate hasta,
            @Param("sucursalId") Long sucursalId,
            @Param("cancelada") LiquidacionSueldo.EstadoLiquidacion cancelada
    );
}
