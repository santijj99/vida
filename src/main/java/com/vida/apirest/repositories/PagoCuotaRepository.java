package com.vida.apirest.repositories;

import com.vida.apirest.model.credito.Cuota;
import com.vida.apirest.model.credito.PagoCuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PagoCuotaRepository extends JpaRepository<PagoCuota, Long> {

    @Query("""
            SELECT p FROM PagoCuota p
            JOIN FETCH p.cuota c
            JOIN FETCH c.credito cr
            WHERE cr.cliente.id = (
                SELECT cc.cliente.id FROM Cuenta cc WHERE cc.id = :cuentaId
            )
            ORDER BY p.createdAt DESC
            """)
    List<PagoCuota> findByCuentaIdOrderByCreatedAtDesc(@Param("cuentaId") Long cuentaId);

    @Query("""
            SELECT c FROM Cuota c
            JOIN FETCH c.credito cr
            WHERE cr.cliente.id = :clienteId
            AND c.estado = :estadoPagada
            AND NOT EXISTS (
                SELECT p FROM PagoCuota p
                WHERE p.cuota = c
                AND p.estado = :estadoPagoActivo
            )
            """)
    List<Cuota> findPagadasSinPagoActivo(
            @Param("clienteId") Long clienteId,
            @Param("estadoPagada") Cuota.EstadoCuota estadoPagada,
            @Param("estadoPagoActivo") PagoCuota.EstadoPagoCuota estadoPagoActivo
    );

    @Query("""
            SELECT p FROM PagoCuota p
            JOIN FETCH p.cuota c
            JOIN FETCH c.credito cr
            JOIN FETCH cr.cliente cl
            LEFT JOIN FETCH cr.sucursal s
            LEFT JOIN FETCH p.movimientoFinanciero
            WHERE p.id = :id
            """)
    Optional<PagoCuota> findByIdWithDetalle(@Param("id") Long id);
}
