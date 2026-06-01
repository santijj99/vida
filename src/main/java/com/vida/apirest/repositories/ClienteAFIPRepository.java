package com.vida.apirest.repositories;

import com.vida.apirest.model.afip.ClienteAFIP;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteAFIPRepository extends JpaRepository<ClienteAFIP, Long> {
    Optional<ClienteAFIP> findByCliente_Id(Long clienteId);

    Optional<ClienteAFIP> findFirstByDocTipoAndDocNroOrderByIdClienteAFIPDesc(Integer docTipo, String docNro);
}
