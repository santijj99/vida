package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.CreateGeneroRequest;
import com.vida.apirest.dto.ariticulo.GeneroResponse;
import com.vida.apirest.model.articulo.Genero;
import com.vida.apirest.repositories.GeneroRepository;
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
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new RuntimeException("El nombre del género es obligatorio");
        }

        String nombre = request.getNombre().trim();
        if (generoRepository.findByNombre(nombre).isPresent()) {
            throw new RuntimeException("Ya existe un género con el nombre: " + nombre);
        }

        Genero genero = new Genero();
        genero.setNombre(nombre);
        genero.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        genero.setActivo(request.getActivo() != null ? request.getActivo() : true);

        return toResponse(generoRepository.save(genero));
    }

    private GeneroResponse toResponse(Genero genero) {
        GeneroResponse response = new GeneroResponse();
        response.setId(genero.getId());
        response.setNombre(genero.getNombre());
        response.setDescripcion(genero.getDescripcion());
        response.setActivo(genero.getActivo());
        response.setCreatedAt(genero.getCreatedAt());
        response.setUpdatedAt(genero.getUpdatedAt());
        return response;
    }
}
