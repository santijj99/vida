package com.vida.apirest.repositories;

import com.vida.apirest.model.credito.CreditoConfigEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CreditoConfigEmpresaRepository extends JpaRepository<CreditoConfigEmpresa, Long> {

    @Query("SELECT c FROM CreditoConfigEmpresa c JOIN FETCH c.empresa WHERE c.empresa.id = :empresaId")
    Optional<CreditoConfigEmpresa> findByEmpresaId(Long empresaId);
}
