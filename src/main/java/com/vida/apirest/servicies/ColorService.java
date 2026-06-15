package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.ColorResponse;
import com.vida.apirest.dto.ariticulo.CreateColorRequest;
import com.vida.apirest.repositories.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ColorService {

    private final ColorRepository colorRepository;

    @Transactional(readOnly = true)
    public List<ColorResponse> findAll() {
        return colorRepository.findAllByOrderByNombreAsc().stream()
                .map(color -> {
                    ColorResponse response = new ColorResponse();
                    response.setId(color.getId());
                    response.setNombre(color.getNombre());
                    return response;
                })
                .toList();
    }

    @Transactional
    public ColorResponse create(CreateColorRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new RuntimeException("El nombre del color es obligatorio");
        }

        String nombre = request.getNombre().trim();
        if (colorRepository.findByNombre(nombre).isPresent()) {
            throw new RuntimeException("Ya existe un color con el nombre: " + nombre);
        }

        com.vida.apirest.model.articulo.Color color = new com.vida.apirest.model.articulo.Color();
        color.setNombre(nombre);

        color = colorRepository.save(color);

        ColorResponse response = new ColorResponse();
        response.setId(color.getId());
        response.setNombre(color.getNombre());
        return response;
    }
}
