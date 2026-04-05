package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.GastoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GastoCategoriaRepository extends JpaRepository<GastoCategoria, Long> {
}
