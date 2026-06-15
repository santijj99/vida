package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Talle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface TalleRepository extends JpaRepository<Talle, Long> {
    Optional<Talle> findByPaisAndNumero(Talle.Pais pais, String numero);

    List<Talle> findAllByOrderByPaisAscNumeroAsc();

    @Query("SELECT DISTINCT t.numero FROM Talle t ORDER BY t.numero ASC")
    List<String> findDistinctNumeros();
}
