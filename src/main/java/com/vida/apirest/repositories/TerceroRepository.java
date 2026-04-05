package com.vida.apirest.repositories;

import com.vida.apirest.model.terceros.Tercero;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerceroRepository extends JpaRepository<Tercero, Long> {
}
