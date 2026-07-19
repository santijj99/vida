package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.ColorResponse;
import com.vida.apirest.dto.ariticulo.CreateColorRequest;
import com.vida.apirest.model.articulo.Color;
import com.vida.apirest.repositories.ColorRepository;
import com.vida.apirest.utils.ValidationUtils;
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
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ColorResponse create(CreateColorRequest request) {
        String nombre = ValidationUtils.requireNonBlank(
                request.getNombre(), "El nombre del color es obligatorio");
        ValidationUtils.assertUnique(
                () -> colorRepository.findByNombre(nombre),
                "Ya existe un color con el nombre: " + nombre);

        Color color = new Color();
        color.setNombre(nombre);
        return toResponse(colorRepository.save(color));
    }

    private ColorResponse toResponse(Color color) {
        ColorResponse response = new ColorResponse();
        response.setId(color.getId());
        response.setNombre(color.getNombre());
        return response;
    }
}
