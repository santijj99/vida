package com.vida.apirest.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vida.apirest.model.articulo.VarianteArticulo;

public interface VarianteArticuloRepository extends JpaRepository<VarianteArticulo, Long> {

    boolean existsByCodigoBarras(String codigoBarras);
boolean existsByArticuloIdAndColorIdAndTalleIdAndEstado(Long articuloId, Long colorId, Long talleId, VarianteArticulo.EstadoVariante estado);

}
