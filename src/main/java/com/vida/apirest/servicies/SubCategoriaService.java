package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.CreateSubCategoriaRequest;
import com.vida.apirest.dto.ariticulo.SubCategoriaResponse;
import com.vida.apirest.model.articulo.Taxon;
import com.vida.apirest.repositories.TaxonRepository;
import com.vida.apirest.utils.CatalogMapper;
import com.vida.apirest.utils.ValidationUtils;
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
        String nombre = ValidationUtils.requireNonBlank(
                request.getNombre(), "El nombre de la subcategoría es obligatorio");
        ValidationUtils.assertUnique(
                () -> taxonRepository.findByNombre(nombre),
                "Ya existe una subcategoría con el nombre: " + nombre);

        Taxon taxon = new Taxon();
        taxon.setNombre(nombre);
        taxon.setDescripcion(ValidationUtils.trimToNull(request.getDescripcion()));
        taxon.setNivel(1);
        taxon.setActivo(ValidationUtils.defaultActivo(request.getActivo()));

        return toResponse(taxonRepository.save(taxon));
    }

    private SubCategoriaResponse toResponse(Taxon taxon) {
        return CatalogMapper.mapAuditable(new SubCategoriaResponse(), taxon);
    }
}
