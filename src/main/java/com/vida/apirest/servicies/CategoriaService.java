package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.CategoriaResponse;
import com.vida.apirest.dto.ariticulo.CreateCategoriaRequest;
import com.vida.apirest.model.articulo.Categoria;
import com.vida.apirest.repositories.CategoriaRepository;
import com.vida.apirest.utils.CatalogMapper;
import com.vida.apirest.utils.ValidationUtils;
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
        String nombre = ValidationUtils.requireNonBlank(
                request.getNombre(), "El nombre de la categoría es obligatorio");
        ValidationUtils.assertUnique(
                () -> categoriaRepository.findByNombre(nombre),
                "Ya existe una categoría con el nombre: " + nombre);

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(ValidationUtils.trimToNull(request.getDescripcion()));
        categoria.setActivo(ValidationUtils.defaultActivo(request.getActivo()));

        return toResponse(categoriaRepository.save(categoria));
    }

    private CategoriaResponse toResponse(Categoria categoria) {
        return CatalogMapper.mapAuditable(new CategoriaResponse(), categoria);
    }
}
