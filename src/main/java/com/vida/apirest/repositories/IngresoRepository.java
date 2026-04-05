package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.Ingreso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngresoRepository extends JpaRepository<Ingreso, Long> {
}
