package com.vida.apirest.repositories;

import com.vida.apirest.model.credito.Credito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreditoRepository extends JpaRepository<Credito, Long> {

    /** Sin JOIN FETCH de cuotas: DISTINCT+OneToMany devuelve un solo crédito incorrectamente. */
    @Query("SELECT c FROM Credito c WHERE c.cliente.id = :clienteId ORDER BY c.createdAt DESC")
    List<Credito> findByClienteIdOrderByCreatedAtDesc(@Param("clienteId") Long clienteId);
}
