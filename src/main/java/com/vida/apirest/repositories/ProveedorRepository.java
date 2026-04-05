package com.vida.apirest.repositories;

import com.vida.apirest.model.persona.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
}
