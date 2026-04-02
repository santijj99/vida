package com.vida.apirest.repositories;

import com.vida.apirest.model.Contacto;
import com.vida.apirest.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactoRepository extends JpaRepository<Contacto, Long> {
}
