package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.ListaPrecio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListaPrecioRepository extends JpaRepository<ListaPrecio, Long> {
}
