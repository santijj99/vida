package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.GastoPago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GastoPagoRepository extends JpaRepository<GastoPago, Long> {
}
