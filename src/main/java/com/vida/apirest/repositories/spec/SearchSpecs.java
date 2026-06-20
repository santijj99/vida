package com.vida.apirest.repositories.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SearchSpecs {

    private SearchSpecs() {
    }

    public static Specification<Object> textContainsAny(String query, Expression<String>... fields) {
        return (root, criteriaQuery, cb) -> likeAny(cb, normalizeQuery(query), fields);
    }

    public static String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim();
    }

    public static boolean isBlank(String query) {
        return query == null || query.isBlank();
    }

    @SafeVarargs
    public static Predicate likeAny(CriteriaBuilder cb, String query, Expression<String>... fields) {
        if (isBlank(query)) {
            return cb.conjunction();
        }
        String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
        List<Predicate> predicates = new ArrayList<>();
        for (Expression<String> field : fields) {
            predicates.add(cb.like(cb.lower(cb.coalesce(field, "")), pattern));
        }
        return cb.or(predicates.toArray(Predicate[]::new));
    }
}
