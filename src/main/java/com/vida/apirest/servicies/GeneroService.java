package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.CreateGeneroRequest;
import com.vida.apirest.dto.ariticulo.GeneroResponse;
import com.vida.apirest.model.articulo.Genero;
import com.vida.apirest.repositories.GeneroRepository;
import com.vida.apirest.utils.CatalogMapper;
import com.vida.apirest.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeneroService {

    private final GeneroRepository generoRepository;

    @Transactional(readOnly = true)
    public List<GeneroResponse> findAll() {
        return generoRepository.findAllByOrderByNombreAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GeneroResponse create(CreateGeneroRequest request) {
        String nombre = ValidationUtils.requireNonBlank(
                request.getNombre(), "El nombre del género es obligatorio");
        ValidationUtils.assertUnique(
                () -> generoRepository.findByNombre(nombre),
                "Ya existe un género con el nombre: " + nombre);

        Genero genero = new Genero();
        genero.setNombre(nombre);
        genero.setDescripcion(ValidationUtils.trimToNull(request.getDescripcion()));
        genero.setActivo(ValidationUtils.defaultActivo(request.getActivo()));

        return toResponse(generoRepository.save(genero));
    }

    private GeneroResponse toResponse(Genero genero) {
        return CatalogMapper.mapAuditable(new GeneroResponse(), genero);
    }
}
