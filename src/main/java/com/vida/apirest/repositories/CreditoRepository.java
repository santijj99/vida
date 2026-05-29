package com.vida.apirest.repositories;

import com.vida.apirest.model.credito.Credito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CreditoRepository extends JpaRepository<Credito, Long> {

    /** Sin JOIN FETCH de cuotas: DISTINCT+OneToMany devuelve un solo crédito incorrectamente. */
    @Query("SELECT c FROM Credito c WHERE c.cliente.id = :clienteId ORDER BY c.createdAt DESC")
    List<Credito> findByClienteIdOrderByCreatedAtDesc(@Param("clienteId") Long clienteId);

    List<Credito> findByVentaId(Long ventaId);

    @Query("""
            SELECT c.cliente.id AS clienteId,
                   COALESCE(SUM(c.importe), 0) AS totalCreditosSacados,
                   COALESCE(SUM(c.importe - COALESCE(c.saldo, 0)), 0) AS totalPagado,
                   COUNT(c) AS cantidadCreditos
            FROM Credito c
            WHERE c.cliente.id IN :clienteIds
            AND c.estado <> com.vida.apirest.model.credito.Credito.EstadoCredito.CANCELADO
            GROUP BY c.cliente.id
            """)
    List<CreditoResumenPorCliente> resumenPorClientes(@Param("clienteIds") Collection<Long> clienteIds);

    @Query("""
            SELECT DISTINCT c.cliente.id FROM Credito c
            WHERE c.cliente.id IN :clienteIds
            AND c.estado = com.vida.apirest.model.credito.Credito.EstadoCredito.VENCIDO
            """)
    List<Long> findClienteIdsConCreditosVencidos(@Param("clienteIds") Collection<Long> clienteIds);
}
