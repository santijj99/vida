package com.vida.apirest.repositories;

import com.vida.apirest.model.config.PreferenciaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreferenciaUsuarioRepository extends JpaRepository<PreferenciaUsuario, Long> {

    Optional<PreferenciaUsuario> findByUsuarioIdAndClave(Long usuarioId, String clave);

    Optional<PreferenciaUsuario> findByUsuarioIdIsNullAndClave(String clave);
}
