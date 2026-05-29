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

	@Query("""
			SELECT DISTINCT a FROM Articulo a
			LEFT JOIN FETCH a.marca
			LEFT JOIN FETCH a.categoria
			LEFT JOIN FETCH a.genero
			LEFT JOIN FETCH a.variantes v
			LEFT JOIN FETCH v.color
			LEFT JOIN FETCH v.talle
			LEFT JOIN FETCH v.taxon
			ORDER BY a.codigo ASC, v.id ASC
			""")
	List<Articulo> findAllWithDetalle();

	@Query("""
			SELECT DISTINCT a FROM Articulo a
			LEFT JOIN FETCH a.marca
			LEFT JOIN FETCH a.variantes v
			LEFT JOIN FETCH v.color
			LEFT JOIN FETCH v.talle
			WHERE a.id = :id
			""")
	Optional<Articulo> findByIdWithDetalle(@Param("id") Long id);
}
