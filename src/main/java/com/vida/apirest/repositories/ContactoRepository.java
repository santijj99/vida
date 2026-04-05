package com.vida.apirest.repositories;

import com.vida.apirest.model.persona.Contacto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactoRepository extends JpaRepository<Contacto, Long> {
}
