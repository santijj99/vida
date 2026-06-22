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

    boolean existsByUsuario_IdAndSucursal_Id(Long usuarioId, Long sucursalId);
}
