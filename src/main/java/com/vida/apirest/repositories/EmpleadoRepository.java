package com.vida.apirest.repositories;

import com.vida.apirest.model.persona.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
}
