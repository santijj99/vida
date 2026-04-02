package com.vida.apirest.repositories;

import com.vida.apirest.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, String> {
    boolean existsByNombre(String name);

    List<Role> findAllByUsuariosHasRoles_Usuario_Id(Long idUsuario);
}
