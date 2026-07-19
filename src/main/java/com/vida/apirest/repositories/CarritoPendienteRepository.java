package com.vida.apirest.repositories;

import com.vida.apirest.model.venta.CarritoPendiente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CarritoPendienteRepository extends JpaRepository<CarritoPendiente, Long> {

    @Query("""
            SELECT DISTINCT c FROM CarritoPendiente c
            JOIN FETCH c.cliente
            JOIN FETCH c.sucursal
            LEFT JOIN FETCH c.empleado
            LEFT JOIN FETCH c.detalles d
            LEFT JOIN FETCH d.articulo
            LEFT JOIN FETCH d.variante
            WHERE (:sucursalId IS NULL OR c.sucursal.id = :sucursalId)
              AND c.estado IN :estados
            ORDER BY c.createdAt DESC
            """)
    List<CarritoPendiente> findAllWithDetalles(
            @Param("sucursalId") Long sucursalId,
            @Param("estados") List<CarritoPendiente.EstadoCarrito> estados);

    @Query("""
            SELECT c FROM CarritoPendiente c
            JOIN FETCH c.cliente
            JOIN FETCH c.sucursal
            LEFT JOIN FETCH c.empleado
            LEFT JOIN FETCH c.detalles d
            LEFT JOIN FETCH d.articulo a
            LEFT JOIN FETCH a.marca
            LEFT JOIN FETCH d.variante v
            LEFT JOIN FETCH v.talle
            LEFT JOIN FETCH v.color
            LEFT JOIN FETCH c.venta
            WHERE c.id = :id
            """)
    Optional<CarritoPendiente> findByIdWithDetalles(@Param("id") Long id);
}
