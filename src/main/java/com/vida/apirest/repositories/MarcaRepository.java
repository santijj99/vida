package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Marca;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarcaRepository extends JpaRepository<Marca, Long> {
}
