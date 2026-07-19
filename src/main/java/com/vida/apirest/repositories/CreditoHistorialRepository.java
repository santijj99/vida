package com.vida.apirest.repositories;

import com.vida.apirest.model.credito.CreditoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditoHistorialRepository extends JpaRepository<CreditoHistorial, Long> {

    List<CreditoHistorial> findByCreditoIdOrderByCreatedAtDesc(Long creditoId);
}
