package com.vida.apirest.repositories;

import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.repositories.spec.ClienteSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    @EntityGraph(attributePaths = {"direccion", "garante", "contactos"})
    Optional<Cliente> findFirstByDniOrderByIdAsc(String dni);

    @Deprecated
    default Optional<Cliente> findByDni(String dni) {
        return findFirstByDniOrderByIdAsc(dni);
    }

    @EntityGraph(attributePaths = {"direccion", "garante", "contactos"})
    Optional<Cliente> findWithRelationsById(Long id);

    @EntityGraph(attributePaths = {"direccion", "garante", "contactos"})
    @Query("SELECT c FROM Cliente c ORDER BY c.apellido ASC, c.nombre ASC")
    List<Cliente> findAllWithRelations();

    @EntityGraph(attributePaths = {"direccion", "garante", "contactos"})
    Page<Cliente> findAll(Specification<Cliente> spec, Pageable pageable);

    default Page<Cliente> searchPage(String q, Pageable pageable) {
        return findAll(ClienteSpecs.matchesQuery(q), pageable);
    }
}
