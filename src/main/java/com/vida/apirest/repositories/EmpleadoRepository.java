package com.vida.apirest.repositories;

import com.vida.apirest.model.persona.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Optional<Empleado> findByDni(String dni);
    Optional<Empleado> findFirstByDniOrderByIdAsc(String dni);
    Optional<Empleado> findByUsuario_Id(Long usuarioId);

    @Query("""
            SELECT e FROM Empleado e
            WHERE e.usuario IS NOT NULL
            AND e.activo = true
            AND e.usuario.id NOT IN (
                SELECT us.usuario.id FROM UsuarioSucursal us WHERE us.sucursal.id = :sucursalId
            )
            ORDER BY e.apellido ASC, e.nombre ASC
            """)
    List<Empleado> findDisponiblesParaSucursal(@Param("sucursalId") Long sucursalId);
}
