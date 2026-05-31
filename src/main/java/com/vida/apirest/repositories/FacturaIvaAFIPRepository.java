package com.vida.apirest.repositories;

import com.vida.apirest.model.afip.FacturaIvaAFIP;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacturaIvaAFIPRepository extends JpaRepository<FacturaIvaAFIP, Long> {
    List<FacturaIvaAFIP> findByFacturaAFIP_IdFacturaAFIP(Long idFacturaAFIP);
}
