package com.vida.apirest.repositories;

import com.vida.apirest.model.auth.UsuarioHasRoles;
import com.vida.apirest.model.auth.id.UsuarioRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioHasRoleRepository extends JpaRepository <UsuarioHasRoles, UsuarioRoleId> {


}
