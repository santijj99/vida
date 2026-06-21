package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.CreateSubCategoriaRequest;
import com.vida.apirest.dto.ariticulo.SubCategoriaResponse;
import com.vida.apirest.dto.ariticulo.CreateClasificacionRequest;
import com.vida.apirest.dto.ariticulo.ClasificacionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Compatibilidad con /api/sub-categorias. Preferir {@link ClasificacionService}. */
@Service
@RequiredArgsConstructor
public class SubCategoriaService {

    private final ClasificacionService clasificacionService;

    @Transactional(readOnly = true)
    public List<SubCategoriaResponse> findAll() {
        return clasificacionService.findAll().stream()
                .map(this::toLegacy)
                .toList();
    }

    @Transactional
    public SubCategoriaResponse create(CreateSubCategoriaRequest request) {
        CreateClasificacionRequest req = new CreateClasificacionRequest();
        req.setNombre(request.getNombre());
        req.setDescripcion(request.getDescripcion());
        req.setActivo(request.getActivo());
        return toLegacy(clasificacionService.create(req));
    }

    private SubCategoriaResponse toLegacy(ClasificacionResponse source) {
        SubCategoriaResponse target = new SubCategoriaResponse();
        target.setId(source.getId());
        target.setNombre(source.getNombre());
        target.setDescripcion(source.getDescripcion());
        target.setActivo(source.getActivo());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }
}
