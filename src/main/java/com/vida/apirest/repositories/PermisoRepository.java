package com.vida.apirest.repositories;

import com.vida.apirest.model.auth.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {
    Optional<Permiso> findByCodigo(String codigo);

    List<Permiso> findByCodigoIn(Collection<String> codigos);

    List<Permiso> findAllByOrderByModuloAscCodigoAsc();
}
