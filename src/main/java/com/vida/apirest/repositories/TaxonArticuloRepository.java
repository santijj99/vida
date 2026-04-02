package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.TaxonArticulo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxonArticuloRepository extends JpaRepository<TaxonArticulo, Long> {
}
