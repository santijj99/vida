package com.vida.apirest.repositories;

import com.vida.apirest.model.auth.UsuarioHasRoles;
import com.vida.apirest.model.auth.id.UsuarioRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioHasRoleRepository extends JpaRepository<UsuarioHasRoles, UsuarioRoleId> {

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UsuarioHasRoles u WHERE u.usuario.id = :usuarioId AND u.role.id = :rolId")
    boolean existsByUsuarioIdAndRoleId(@Param("usuarioId") Long usuarioId, @Param("rolId") Long rolId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UsuarioHasRoles u WHERE u.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);
}
