package com.vida.apirest.repositories;

import com.vida.apirest.model.almacen.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {
    Optional<Sucursal> findByCodigo(String codigo);
    Optional<Sucursal> findFirstByOrderByIdAsc();
}
