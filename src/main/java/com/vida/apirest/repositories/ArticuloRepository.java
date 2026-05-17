package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticuloRepository extends JpaRepository<Articulo, Long>, JpaSpecificationExecutor<Articulo> {

	Optional<Articulo> findByCodigo(String codigo);

	List<Articulo> findAllByMarcaNombreContainingIgnoreCase(String nombre);

	@Query("select distinct a from Articulo a join a.variantes v join v.talle t where t.numero = :numero")
	List<Articulo> findAllByTalleNumero(@Param("numero") String numero);

	@Query("select distinct a from Articulo a join a.variantes v join v.color c where lower(c.nombre) like lower(concat('%', :nombre, '%'))")
	List<Articulo> findAllByColorNombreContaining(@Param("nombre") String nombre);
}
