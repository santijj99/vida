package com.vida.apirest.repositories;

import com.vida.apirest.model.sueldo.EmpleadoSueldoConfig;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmpleadoSueldoConfigRepository extends JpaRepository<EmpleadoSueldoConfig, Long> {

    @EntityGraph(attributePaths = {"empleado", "empleado.usuario"})
    Optional<EmpleadoSueldoConfig> findByEmpleado_Id(Long empleadoId);

    boolean existsByEmpleado_Id(Long empleadoId);

    @EntityGraph(attributePaths = {"empleado", "empleado.usuario"})
    @Query("SELECT c FROM EmpleadoSueldoConfig c ORDER BY c.empleado.apellido ASC, c.empleado.nombre ASC")
    List<EmpleadoSueldoConfig> findAllWithEmpleado();

    @EntityGraph(attributePaths = {"empleado", "empleado.usuario"})
    @Query("""
            SELECT c FROM EmpleadoSueldoConfig c
            WHERE c.activo = true AND c.empleado.activo = true
            ORDER BY c.empleado.apellido ASC, c.empleado.nombre ASC
            """)
    List<EmpleadoSueldoConfig> findActivas();
}
