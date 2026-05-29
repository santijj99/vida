package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.Moneda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonedaRepository extends JpaRepository<Moneda, Long> {
    Optional<Moneda> findByCodigo(String codigo);
    List<Moneda> findByPredeterminadaTrue();
    List<Moneda> findByActivoTrue();
}
