package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.Moneda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonedaRepository extends JpaRepository<Moneda, Long> {
}
