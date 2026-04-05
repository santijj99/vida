package com.vida.apirest.repositories;

import com.vida.apirest.model.persona.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
