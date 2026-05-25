package com.vida.apirest.repositories;

import com.vida.apirest.model.credito.Cuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CuotaRepository extends JpaRepository<Cuota, Long> {

    @Query("SELECT q FROM Cuota q JOIN FETCH q.credito c JOIN FETCH c.cliente JOIN FETCH c.sucursal WHERE q.id IN :ids")
    List<Cuota> findByIdInWithCredito(@Param("ids") List<Long> ids);

    @Query("SELECT q FROM Cuota q WHERE q.credito.id IN :creditoIds ORDER BY q.credito.id, q.fechaVencimiento")
    List<Cuota> findByCreditoIdIn(@Param("creditoIds") List<Long> creditoIds);
}
