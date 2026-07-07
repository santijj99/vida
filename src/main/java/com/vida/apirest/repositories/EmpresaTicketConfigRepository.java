package com.vida.apirest.repositories;

import com.vida.apirest.model.empresa.EmpresaTicketConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmpresaTicketConfigRepository extends JpaRepository<EmpresaTicketConfig, Long> {

    @Query("SELECT c FROM EmpresaTicketConfig c JOIN FETCH c.empresa WHERE c.empresa.id = :empresaId")
    Optional<EmpresaTicketConfig> findByEmpresaId(Long empresaId);
}
