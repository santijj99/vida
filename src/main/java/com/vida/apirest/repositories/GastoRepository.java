package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GastoRepository extends JpaRepository<Gasto, Long> {
}
