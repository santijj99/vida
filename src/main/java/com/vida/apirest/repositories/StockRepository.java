package com.vida.apirest.repositories;

import com.vida.apirest.model.almacen.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findFirstByArticuloIdAndVarianteIdAndSucursalIdOrderByIdAsc(
            Long articuloId, Long varianteId, Long sucursalId);
    Optional<Stock> findFirstByArticuloIdAndSucursalIdOrderByIdAsc(Long articuloId, Long sucursalId);
    Optional<Stock> findFirstByVarianteIdAndSucursalIdOrderByIdAsc(Long varianteId, Long sucursalId);

    @Deprecated
    default Optional<Stock> findByArticuloIdAndVarianteIdAndSucursalId(
            Long articuloId, Long varianteId, Long sucursalId) {
        return findFirstByArticuloIdAndVarianteIdAndSucursalIdOrderByIdAsc(articuloId, varianteId, sucursalId);
    }

    @Deprecated
    default Optional<Stock> findByArticuloIdAndSucursalId(Long articuloId, Long sucursalId) {
        return findFirstByArticuloIdAndSucursalIdOrderByIdAsc(articuloId, sucursalId);
    }

    @Deprecated
    default Optional<Stock> findByVarianteIdAndSucursalId(Long varianteId, Long sucursalId) {
        return findFirstByVarianteIdAndSucursalIdOrderByIdAsc(varianteId, sucursalId);
    }
    Optional<Stock> findByDepositoIdAndArticuloIdAndVarianteId(Long depositoId, Long articuloId, Long varianteId);
    List<Stock> findAllByArticulo_IdAndVariante_Id(Long articuloId, Long varianteId);

    @Query("""
            SELECT s FROM Stock s
            JOIN FETCH s.articulo
            LEFT JOIN FETCH s.variante
            JOIN FETCH s.sucursal
            ORDER BY s.id
            """)
    List<Stock> findAllWithRelations();

    /** Bloqueo pesimista para serializar cobros/reservas/transferencias sobre la misma fila. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.id = :id")
    Optional<Stock> findByIdForUpdate(@Param("id") Long id);
}
