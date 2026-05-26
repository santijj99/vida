package com.vida.apirest.repositories;

import com.vida.apirest.model.venta.PrestamoCondicional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrestamoCondicionalRepository extends JpaRepository<PrestamoCondicional, Long> {

    @Query("""
            SELECT DISTINCT p FROM PrestamoCondicional p
            JOIN FETCH p.cliente
            JOIN FETCH p.sucursal
            LEFT JOIN FETCH p.detalles d
            LEFT JOIN FETCH d.articulo
            LEFT JOIN FETCH d.variante
            WHERE (:sucursalId IS NULL OR p.sucursal.id = :sucursalId)
              AND p.estado IN :estados
            ORDER BY p.fechaEntrega DESC
            """)
    List<PrestamoCondicional> findAllWithDetalles(
            @Param("sucursalId") Long sucursalId,
            @Param("estados") List<PrestamoCondicional.EstadoPrestamo> estados);

    @Query("""
            SELECT p FROM PrestamoCondicional p
            JOIN FETCH p.cliente
            JOIN FETCH p.sucursal
            LEFT JOIN FETCH p.detalles d
            LEFT JOIN FETCH d.articulo
            LEFT JOIN FETCH d.variante
            LEFT JOIN FETCH p.venta
            WHERE p.id = :id
            """)
    Optional<PrestamoCondicional> findByIdWithDetalles(@Param("id") Long id);
}
