package com.vida.apirest.repositories;

import com.vida.apirest.model.auth.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByNombre(String name);
    Optional<Role> findByNombre(String nombre);

    List<Role> findAllByUsuariosHasRoles_Usuario_Id(Long idUsuario);
}
