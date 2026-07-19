package com.vida.apirest.repositories;

import com.vida.apirest.model.auth.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);
    boolean existsByUsuario(String usuario);
    boolean existsByCelular(String celular);
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByUsuario(String usuario);

    @Query("""
            SELECT u FROM Usuario u
            LEFT JOIN FETCH u.usuarioHasRoles uhr
            LEFT JOIN FETCH uhr.role
            LEFT JOIN FETCH u.rolPrincipal
            WHERE u.email = :email
            """)
    Optional<Usuario> findByEmailWithRolesAndRolPrincipal(@Param("email") String email);

    @Query("""
            SELECT u FROM Usuario u
            LEFT JOIN FETCH u.usuarioHasRoles uhr
            LEFT JOIN FETCH uhr.role
            LEFT JOIN FETCH u.rolPrincipal
            WHERE lower(u.email) = lower(:identificador) OR lower(u.usuario) = lower(:identificador)
            """)
    Optional<Usuario> findByIdentificadorWithRolesAndRolPrincipal(@Param("identificador") String identificador);

    @Query("""
            SELECT DISTINCT u FROM Usuario u
            LEFT JOIN FETCH u.usuarioHasRoles uhr
            LEFT JOIN FETCH uhr.role
            LEFT JOIN FETCH u.rolPrincipal
            ORDER BY u.id
            """)
    List<Usuario> findAllWithRolesAndRolPrincipal();
}
