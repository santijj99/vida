package com.vida.apirest.repositories;

import com.vida.apirest.model.UsuarioHasRoles;
import com.vida.apirest.model.id.UsuarioRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioHasRoleRepository extends JpaRepository <UsuarioHasRoles, UsuarioRoleId> {


}
