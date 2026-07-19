package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.HistorialPrecio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HistorialPrecioRepository extends JpaRepository<HistorialPrecio, Long> {

    Optional<HistorialPrecio> findFirstByVarianteArticuloIdOrderByFechaDesc(Long varianteArticuloId);

    @Query(value = """
            SELECT DISTINCT ON (variante_articulo_id)
                variante_articulo_id,
                precio_nuevo
            FROM historial_precio
            WHERE variante_articulo_id IN (:varianteIds)
            ORDER BY variante_articulo_id, fecha DESC
            """, nativeQuery = true)
    List<Object[]> findLatestPreciosByVarianteIds(@Param("varianteIds") Collection<Long> varianteIds);
}
