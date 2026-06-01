package com.vida.apirest.repositories;

import com.vida.apirest.model.afip.FacturaAFIP;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacturaAFIPRepository extends JpaRepository<FacturaAFIP, Long> {
    Optional<FacturaAFIP> findByVenta_Id(Long ventaId);

    List<FacturaAFIP> findAllByOrderByFechaEmisionDesc();
}
