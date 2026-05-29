package com.vida.apirest.repositories;

import com.vida.apirest.model.auth.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);
    Optional<Usuario> findByEmail(String email);

    @Query("""
            SELECT u FROM Usuario u
            LEFT JOIN FETCH u.usuarioHasRoles uhr
            LEFT JOIN FETCH uhr.role
            LEFT JOIN FETCH u.rolPrincipal
            WHERE u.email = :email
            """)
    Optional<Usuario> findByEmailWithRolesAndRolPrincipal(@Param("email") String email);
}
