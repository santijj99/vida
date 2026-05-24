package com.vida.apirest.repositories;

import com.vida.apirest.model.persona.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DireccionRepository extends JpaRepository<Direccion, Long> {
}
