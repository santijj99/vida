package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.CreateSubCategoriaRequest;
import com.vida.apirest.dto.ariticulo.SubCategoriaResponse;
import com.vida.apirest.model.articulo.Taxon;
import com.vida.apirest.repositories.TaxonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubCategoriaService {

    private final TaxonRepository taxonRepository;

    @Transactional(readOnly = true)
    public List<SubCategoriaResponse> findAll() {
        return taxonRepository.findAllByOrderByNombreAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SubCategoriaResponse create(CreateSubCategoriaRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new RuntimeException("El nombre de la subcategoría es obligatorio");
        }

        String nombre = request.getNombre().trim();
        if (taxonRepository.findByNombre(nombre).isPresent()) {
            throw new RuntimeException("Ya existe una subcategoría con el nombre: " + nombre);
        }

        Taxon taxon = new Taxon();
        taxon.setNombre(nombre);
        taxon.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        taxon.setNivel(1);
        taxon.setActivo(request.getActivo() != null ? request.getActivo() : true);

        return toResponse(taxonRepository.save(taxon));
    }

    private SubCategoriaResponse toResponse(Taxon taxon) {
        SubCategoriaResponse response = new SubCategoriaResponse();
        response.setId(taxon.getId());
        response.setNombre(taxon.getNombre());
        response.setDescripcion(taxon.getDescripcion());
        response.setActivo(taxon.getActivo());
        response.setCreatedAt(taxon.getCreatedAt());
        response.setUpdatedAt(taxon.getUpdatedAt());
        return response;
    }
}
