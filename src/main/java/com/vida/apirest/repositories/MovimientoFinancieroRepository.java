package com.vida.apirest.repositories;

import com.vida.apirest.model.tesoreria.MovimientoFinanciero;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoFinancieroRepository extends JpaRepository<MovimientoFinanciero, Long> {
}
