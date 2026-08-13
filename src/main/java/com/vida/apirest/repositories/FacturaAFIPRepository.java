package com.vida.apirest.repositories;

import com.vida.apirest.model.afip.FacturaAFIP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FacturaAFIPRepository extends JpaRepository<FacturaAFIP, Long> {
    Optional<FacturaAFIP> findByVenta_Id(Long ventaId);

    List<FacturaAFIP> findAllByOrderByFechaEmisionDesc();

    @Query("""
            SELECT DISTINCT f.venta.id FROM FacturaAFIP f
            WHERE f.venta.id IN :ids
              AND (
                f.resultado = 'A'
                OR (f.cae IS NOT NULL AND f.cae <> '')
              )
            """)
    List<Long> findVentaIdsFacturadas(@Param("ids") Collection<Long> ids);
}
