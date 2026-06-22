package com.vida.apirest.repositories;

import com.vida.apirest.model.persona.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Optional<Empleado> findByDni(String dni);
    Optional<Empleado> findByUsuario_Id(Long usuarioId);
}
