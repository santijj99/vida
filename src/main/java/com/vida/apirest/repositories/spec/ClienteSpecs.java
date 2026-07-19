package com.vida.apirest.repositories.spec;

import com.vida.apirest.model.persona.Cliente;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public final class ClienteSpecs {

    private ClienteSpecs() {
    }

    public static Specification<Cliente> matchesQuery(String query) {
        String normalized = SearchSpecs.normalizeQuery(query);
        return (root, criteriaQuery, cb) -> {
            if (SearchSpecs.isBlank(normalized)) {
                return cb.conjunction();
            }
            Join<Object, Object> garante = root.join("garante", JoinType.LEFT);
            Join<Object, Object> direccion = root.join("direccion", JoinType.LEFT);

            Predicate byNombre = cb.like(cb.lower(cb.coalesce(root.get("nombre"), "")), likePattern(normalized));
            Predicate byApellido = cb.like(cb.lower(cb.coalesce(root.get("apellido"), "")), likePattern(normalized));
            Predicate byDni = cb.like(cb.lower(cb.coalesce(root.get("dni"), "")), likePattern(normalized));
            Predicate byNombreCompleto = cb.like(
                    cb.lower(cb.concat(
                            cb.concat(cb.coalesce(root.get("nombre"), ""), " "),
                            cb.coalesce(root.get("apellido"), ""))),
                    likePattern(normalized));
            Predicate byGarante = cb.like(
                    cb.lower(cb.concat(
                            cb.concat(cb.coalesce(garante.get("nombre"), ""), " "),
                            cb.coalesce(garante.get("apellido"), ""))),
                    likePattern(normalized));
            Predicate byCalle = cb.like(cb.lower(cb.coalesce(direccion.get("calle"), "")), likePattern(normalized));
            Predicate byLocalidad = cb.like(cb.lower(cb.coalesce(direccion.get("localidad"), "")), likePattern(normalized));
            Predicate byBarrio = cb.like(cb.lower(cb.coalesce(direccion.get("barrio"), "")), likePattern(normalized));
            Predicate byProvincia = cb.like(cb.lower(cb.coalesce(direccion.get("provincia"), "")), likePattern(normalized));

            return cb.or(byNombre, byApellido, byDni, byNombreCompleto, byGarante, byCalle, byLocalidad, byBarrio, byProvincia);
        };
    }

    private static String likePattern(String query) {
        return "%" + query.toLowerCase() + "%";
    }
}
