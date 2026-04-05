package com.vida.apirest.repositories;

import com.vida.apirest.model.persona.Garante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GaranteRepository extends JpaRepository<Garante, Long>  {
}
