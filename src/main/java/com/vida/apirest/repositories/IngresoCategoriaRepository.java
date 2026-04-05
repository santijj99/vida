package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.IngresoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngresoCategoriaRepository extends JpaRepository<IngresoCategoria, Long> {
}
