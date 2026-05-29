package com.vida.apirest.repositories;

import com.vida.apirest.model.auth.UsuarioPermisoDeny;
import com.vida.apirest.model.auth.id.UsuarioPermisoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsuarioPermisoDenyRepository extends JpaRepository<UsuarioPermisoDeny, UsuarioPermisoId> {

    List<UsuarioPermisoDeny> findByUsuarioId(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);

    void deleteByPermisoId(Long permisoId);

    @Query("""
            SELECT upd.permiso.codigo FROM UsuarioPermisoDeny upd
            WHERE upd.usuario.id = :usuarioId
            """)
    List<String> findCodigosByUsuarioId(@Param("usuarioId") Long usuarioId);
}
