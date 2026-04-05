package com.vida.apirest.repositories;

import com.vida.apirest.model.almacen.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {
}
