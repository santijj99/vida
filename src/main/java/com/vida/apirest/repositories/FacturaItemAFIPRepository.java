package com.vida.apirest.repositories;

import com.vida.apirest.model.afip.FacturaItemAFIP;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacturaItemAFIPRepository extends JpaRepository<FacturaItemAFIP, Long> {
    List<FacturaItemAFIP> findByFacturaAFIP_IdFacturaAFIP(Long idFacturaAFIP);
}
