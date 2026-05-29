package com.vida.apirest.repositories;

import com.vida.apirest.model.auth.RolePermiso;
import com.vida.apirest.model.auth.id.RolePermisoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermisoRepository extends JpaRepository<RolePermiso, RolePermisoId> {

    List<RolePermiso> findByRoleId(Long roleId);

    void deleteByRoleId(Long roleId);

    void deleteByPermisoId(Long permisoId);

    @Query("""
            SELECT rp.permiso.codigo FROM RolePermiso rp
            WHERE rp.role.id IN :roleIds
            """)
    List<String> findCodigosByRoleIds(@Param("roleIds") List<Long> roleIds);
}
