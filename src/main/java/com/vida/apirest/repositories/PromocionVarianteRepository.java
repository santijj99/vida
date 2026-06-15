package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.PromocionVariante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PromocionVarianteRepository extends JpaRepository<PromocionVariante, Long> {

    @Query("""
            SELECT pv FROM PromocionVariante pv
            JOIN FETCH pv.promocion p
            WHERE pv.variante.id = :varianteId
              AND p.activo = true
              AND (p.fechaInicio IS NULL OR p.fechaInicio <= :hoy)
              AND (p.fechaFin IS NULL OR p.fechaFin >= :hoy)
            """)
    List<PromocionVariante> findActivasByVarianteId(
            @Param("varianteId") Long varianteId,
            @Param("hoy") LocalDate hoy
    );

    @Query("""
            SELECT pv FROM PromocionVariante pv
            JOIN FETCH pv.promocion p
            WHERE pv.variante.id IN :varianteIds
              AND p.activo = true
              AND (p.fechaInicio IS NULL OR p.fechaInicio <= :hoy)
              AND (p.fechaFin IS NULL OR p.fechaFin >= :hoy)
            """)
    List<PromocionVariante> findActivasByVarianteIds(
            @Param("varianteIds") Collection<Long> varianteIds,
            @Param("hoy") LocalDate hoy
    );
}
