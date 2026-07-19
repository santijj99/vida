package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.ClasificacionResponse;
import com.vida.apirest.dto.ariticulo.CreateClasificacionRequest;
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
public class ClasificacionService {

    private final TaxonRepository taxonRepository;

    @Transactional(readOnly = true)
    public List<ClasificacionResponse> findAll() {
        return taxonRepository.findAllByOrderByNombreAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> findNombresActivos() {
        return taxonRepository.findAllByActivoTrueOrderByNombreAsc().stream()
                .map(Taxon::getNombre)
                .toList();
    }

    @Transactional
    public ClasificacionResponse create(CreateClasificacionRequest request) {
        String nombre = ValidationUtils.requireNonBlank(
                request.getNombre(), "El nombre de la clasificación es obligatorio");
        ValidationUtils.assertUnique(
                () -> taxonRepository.findByNombreIgnoreCase(nombre),
                "Ya existe una clasificación con el nombre: " + nombre);

        Taxon taxon = new Taxon();
        taxon.setNombre(nombre.trim());
        taxon.setDescripcion(ValidationUtils.trimToNull(request.getDescripcion()));
        taxon.setNivel(1);
        taxon.setActivo(ValidationUtils.defaultActivo(request.getActivo()));

        return toResponse(taxonRepository.save(taxon));
    }

    @Transactional
    public void seedSiNoExiste(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        String normalizado = nombre.trim();
        if (taxonRepository.findByNombreIgnoreCase(normalizado).isPresent()) {
            return;
        }
        Taxon taxon = new Taxon();
        taxon.setNombre(normalizado);
        taxon.setNivel(1);
        taxon.setActivo(true);
        taxonRepository.save(taxon);
    }

    private ClasificacionResponse toResponse(Taxon taxon) {
        return CatalogMapper.mapAuditable(new ClasificacionResponse(), taxon);
    }
}
