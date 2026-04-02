package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Color;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColorRepository extends JpaRepository<Color, Long> {
}
