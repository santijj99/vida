package com.vida.apirest.repositories;

import com.vida.apirest.model.empresa.EmpresaAfipConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmpresaAfipConfigRepository extends JpaRepository<EmpresaAfipConfig, Long> {

    Optional<EmpresaAfipConfig> findByEmpresaId(Long empresaId);

    @Query("""
            SELECT c FROM EmpresaAfipConfig c
            JOIN FETCH c.empresa e
            WHERE c.afipHabilitado = true AND e.activo = true
            """)
    List<EmpresaAfipConfig> findAllHabilitadasWithEmpresa();

    @Query("""
            SELECT c FROM EmpresaAfipConfig c
            JOIN FETCH c.empresa e
            WHERE e.id = :empresaId
            """)
    Optional<EmpresaAfipConfig> findByEmpresaIdWithEmpresa(@Param("empresaId") Long empresaId);
}
