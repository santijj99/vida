package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.GastoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GastoCategoriaRepository extends JpaRepository<GastoCategoria, Long> {

    List<GastoCategoria> findByActivoTrueOrderByNombreAsc();

    List<GastoCategoria> findAllByOrderByNombreAsc();

    Optional<GastoCategoria> findByCodigo(String codigo);
}
