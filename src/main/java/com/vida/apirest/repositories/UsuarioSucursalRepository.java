package com.vida.apirest.repositories;

import com.vida.apirest.model.auth.UsuarioSucursal;
import com.vida.apirest.model.auth.id.UsuarioSucursalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsuarioSucursalRepository extends JpaRepository<UsuarioSucursal, UsuarioSucursalId> {

    @Query("SELECT us.sucursal.id FROM UsuarioSucursal us WHERE us.usuario.id = :usuarioId")
    List<Long> findSucursalIdsByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT us.sucursal.nombre FROM UsuarioSucursal us WHERE us.usuario.id = :usuarioId ORDER BY us.sucursal.nombre ASC")
    List<String> findSucursalNombresByUsuarioId(@Param("usuarioId") Long usuarioId);

    boolean existsByUsuario_IdAndSucursal_Id(Long usuarioId, Long sucursalId);

    @Query("""
            SELECT e FROM Empleado e
            JOIN e.usuario u
            JOIN UsuarioSucursal us ON us.usuario = u
            WHERE us.sucursal.id = :sucursalId
            ORDER BY e.apellido ASC, e.nombre ASC
            """)
    List<com.vida.apirest.model.persona.Empleado> findEmpleadosBySucursalId(@Param("sucursalId") Long sucursalId);

    void deleteByUsuario_IdAndSucursal_Id(Long usuarioId, Long sucursalId);
}
