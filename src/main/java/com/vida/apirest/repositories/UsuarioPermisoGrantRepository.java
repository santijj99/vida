package com.vida.apirest.repositories;

import com.vida.apirest.model.auth.UsuarioPermisoGrant;
import com.vida.apirest.model.auth.id.UsuarioPermisoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsuarioPermisoGrantRepository extends JpaRepository<UsuarioPermisoGrant, UsuarioPermisoId> {

    List<UsuarioPermisoGrant> findByUsuarioId(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);

    void deleteByPermisoId(Long permisoId);

    @Query("""
            SELECT upg.permiso.codigo FROM UsuarioPermisoGrant upg
            WHERE upg.usuario.id = :usuarioId
            """)
    List<String> findCodigosByUsuarioId(@Param("usuarioId") Long usuarioId);
}
