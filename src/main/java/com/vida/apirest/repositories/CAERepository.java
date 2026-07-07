package com.vida.apirest.repositories;

import com.vida.apirest.model.afip.CAE;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CAERepository extends JpaRepository<CAE, Long> {
    Optional<CAE> findByEmpresaIdAndPtoVtaAndCbteTipo(Long empresaId, Integer ptoVta, Integer cbteTipo);

    Optional<CAE> findByPtoVtaAndCbteTipo(Integer ptoVta, Integer cbteTipo);
}
