package com.vida.apirest.repositories;

import com.vida.apirest.model.venta.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @EntityGraph(attributePaths = {
            "cliente",
            "sucursal",
            "empleado",
            "detalles",
            "detalles.articulo",
            "detalles.articulo.marca",
            "detalles.variante",
            "detalles.variante.talle",
            "detalles.variante.color",
            "pagos"
    })
    @Query("SELECT v FROM Venta v WHERE v.id = :id")
    Optional<Venta> findByIdWithDetalles(@Param("id") Long id);

    @Query(
            value = """
                    SELECT v FROM Venta v
                    JOIN v.cliente c
                    WHERE (:sucursalId IS NULL OR v.sucursal.id = :sucursalId)
                    AND (:estado IS NULL OR CAST(v.estado AS string) = :estado)
                    AND v.fechaVenta >= :desde
                    AND v.fechaVenta < :hasta
                    AND (
                        :q IS NULL OR :q = '' OR
                        LOWER(v.numeroFactura) LIKE LOWER(CONCAT('%', :q, '%')) OR
                        LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR
                        LOWER(c.apellido) LIKE LOWER(CONCAT('%', :q, '%')) OR
                        LOWER(c.dni) LIKE LOWER(CONCAT('%', :q, '%'))
                    )
                    AND (
                        :facturadaArca IS NULL
                        OR (
                            :facturadaArca = TRUE AND EXISTS (
                                SELECT 1 FROM FacturaAFIP f
                                WHERE f.venta = v
                                  AND (f.resultado = 'A' OR (f.cae IS NOT NULL AND f.cae <> ''))
                            )
                        )
                        OR (
                            :facturadaArca = FALSE AND NOT EXISTS (
                                SELECT 1 FROM FacturaAFIP f
                                WHERE f.venta = v
                                  AND (f.resultado = 'A' OR (f.cae IS NOT NULL AND f.cae <> ''))
                            )
                        )
                    )
                    ORDER BY v.fechaVenta DESC
                    """,
            countQuery = """
                    SELECT COUNT(v) FROM Venta v
                    JOIN v.cliente c
                    WHERE (:sucursalId IS NULL OR v.sucursal.id = :sucursalId)
                    AND (:estado IS NULL OR CAST(v.estado AS string) = :estado)
                    AND v.fechaVenta >= :desde
                    AND v.fechaVenta < :hasta
                    AND (
                        :q IS NULL OR :q = '' OR
                        LOWER(v.numeroFactura) LIKE LOWER(CONCAT('%', :q, '%')) OR
                        LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR
                        LOWER(c.apellido) LIKE LOWER(CONCAT('%', :q, '%')) OR
                        LOWER(c.dni) LIKE LOWER(CONCAT('%', :q, '%'))
                    )
                    AND (
                        :facturadaArca IS NULL
                        OR (
                            :facturadaArca = TRUE AND EXISTS (
                                SELECT 1 FROM FacturaAFIP f
                                WHERE f.venta = v
                                  AND (f.resultado = 'A' OR (f.cae IS NOT NULL AND f.cae <> ''))
                            )
                        )
                        OR (
                            :facturadaArca = FALSE AND NOT EXISTS (
                                SELECT 1 FROM FacturaAFIP f
                                WHERE f.venta = v
                                  AND (f.resultado = 'A' OR (f.cae IS NOT NULL AND f.cae <> ''))
                            )
                        )
                    )
                    """
    )
    Page<Venta> searchHistorial(
            @Param("sucursalId") Long sucursalId,
            @Param("estado") String estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("q") String q,
            @Param("facturadaArca") Boolean facturadaArca,
            Pageable pageable
    );

    @Query("SELECT DISTINCT v.sucursal.id FROM Venta v WHERE v.empleado.id = :empleadoId")
    List<Long> findDistinctSucursalIdsByEmpleadoId(@Param("empleadoId") Long empleadoId);

    Optional<Venta> findByClientRequestId(String clientRequestId);
}
