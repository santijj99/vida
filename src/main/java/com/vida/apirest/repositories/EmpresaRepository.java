package com.vida.apirest.repositories;

import com.vida.apirest.model.empresa.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByCodigo(String codigo);

    Optional<Empresa> findByCuit(String cuit);
}
