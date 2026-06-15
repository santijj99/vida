package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.CategoriaResponse;
import com.vida.apirest.dto.ariticulo.CreateCategoriaRequest;
import com.vida.apirest.model.articulo.Categoria;
import com.vida.apirest.repositories.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> findAll() {
        return categoriaRepository.findAllByOrderByNombreAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoriaResponse create(CreateCategoriaRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new RuntimeException("El nombre de la categoría es obligatorio");
        }

        String nombre = request.getNombre().trim();
        if (categoriaRepository.findByNombre(nombre).isPresent()) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + nombre);
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        categoria.setActivo(request.getActivo() != null ? request.getActivo() : true);

        return toResponse(categoriaRepository.save(categoria));
    }

    private CategoriaResponse toResponse(Categoria categoria) {
        CategoriaResponse response = new CategoriaResponse();
        response.setId(categoria.getId());
        response.setNombre(categoria.getNombre());
        response.setDescripcion(categoria.getDescripcion());
        response.setActivo(categoria.getActivo());
        response.setCreatedAt(categoria.getCreatedAt());
        response.setUpdatedAt(categoria.getUpdatedAt());
        return response;
    }
}
