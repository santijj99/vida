package com.vida.apirest.repositories;

import com.vida.apirest.model.almacen.TransferenciaDeStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransferenciaDeStockRepository extends JpaRepository<TransferenciaDeStock, Long> {

    @Query("""
            SELECT DISTINCT t FROM TransferenciaDeStock t
            JOIN FETCH t.depositoOrigen do
            JOIN FETCH do.sucursal so
            JOIN FETCH t.depositoDestino dd
            JOIN FETCH dd.sucursal sd
            LEFT JOIN FETCH t.detalles d
            LEFT JOIN FETCH d.articulo
            LEFT JOIN FETCH d.variante
            WHERE (:depositoOrigenId IS NULL OR t.depositoOrigen.id = :depositoOrigenId)
              AND (:sucursalDestinoId IS NULL OR t.depositoDestino.sucursal.id = :sucursalDestinoId)
            ORDER BY t.createdAt DESC
            """)
    List<TransferenciaDeStock> findAllWithRelations(
            @Param("depositoOrigenId") Long depositoOrigenId,
            @Param("sucursalDestinoId") Long sucursalDestinoId);

    @Query("""
            SELECT t FROM TransferenciaDeStock t
            JOIN FETCH t.depositoOrigen do
            JOIN FETCH do.sucursal so
            JOIN FETCH t.depositoDestino dd
            JOIN FETCH dd.sucursal sd
            LEFT JOIN FETCH t.detalles d
            LEFT JOIN FETCH d.articulo a
            LEFT JOIN FETCH a.marca
            LEFT JOIN FETCH d.variante v
            LEFT JOIN FETCH v.talle
            LEFT JOIN FETCH v.color
            WHERE t.id = :id
            """)
    Optional<TransferenciaDeStock> findByIdWithRelations(@Param("id") Long id);
}
