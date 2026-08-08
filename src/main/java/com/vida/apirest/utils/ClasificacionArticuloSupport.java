package com.vida.apirest.utils;

import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.model.articulo.Taxon;
import com.vida.apirest.model.articulo.TaxonArticulo;
import com.vida.apirest.model.articulo.TaxonArticulo.TipoVinculo;
import com.vida.apirest.repositories.TaxonArticuloRepository;
import com.vida.apirest.servicies.CatalogoResolverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClasificacionArticuloSupport {

    private final CatalogoResolverService catalogoResolverService;
    private final TaxonArticuloRepository taxonArticuloRepository;

    public List<String> normalizarNombres(List<String> nombres) {
        if (nombres == null || nombres.isEmpty()) {
            return List.of();
        }
        Set<String> unicos = new LinkedHashSet<>();
        for (String nombre : nombres) {
            if (nombre == null || nombre.isBlank()) {
                continue;
            }
            unicos.add(nombre.trim());
        }
        return new ArrayList<>(unicos);
    }

    public void sincronizarSubCategoria(Long articuloId, String subCategoria) {
        taxonArticuloRepository.deleteOrphanLinks();
        taxonArticuloRepository.deleteByArticuloIdAndTipo(articuloId, TipoVinculo.SUBCATEGORIA);
        if (subCategoria == null || subCategoria.isBlank()) {
            return;
        }
        Taxon taxon = catalogoResolverService.findOrCreateSubCategoria(subCategoria.trim());
        if (taxon == null) {
            return;
        }
        vincularSiNoExiste(articuloId, taxon.getId(), TipoVinculo.SUBCATEGORIA);
    }

    public void sincronizarClasificaciones(Long articuloId, List<String> clasificaciones) {
        taxonArticuloRepository.deleteOrphanLinks();
        List<String> deseados = normalizarNombres(clasificaciones);
        List<TaxonArticulo> actuales = taxonArticuloRepository.findByArticuloIdAndTipo(
                articuloId, TipoVinculo.CLASIFICACION);
        Set<Long> deseadosIds = new LinkedHashSet<>();

        for (String nombre : deseados) {
            Taxon taxon = catalogoResolverService.findOrCreateClasificacion(nombre);
            if (taxon == null) {
                continue;
            }
            deseadosIds.add(taxon.getId());
            vincularSiNoExiste(articuloId, taxon.getId(), TipoVinculo.CLASIFICACION);
        }

        for (TaxonArticulo link : actuales) {
            if (!deseadosIds.contains(link.getTaxonId())) {
                taxonArticuloRepository.delete(link);
            }
        }
    }

    public String obtenerSubCategoria(Articulo articulo) {
        if (articulo == null || articulo.getTaxones() == null) {
            return null;
        }
        return articulo.getTaxones().stream()
                .filter(ta -> ta.getTipo() == TipoVinculo.SUBCATEGORIA)
                .map(TaxonArticulo::getTaxon)
                .filter(Objects::nonNull)
                .map(Taxon::getNombre)
                .filter(n -> n != null && !n.isBlank())
                .findFirst()
                .orElse(null);
    }

    public List<String> obtenerClasificaciones(Articulo articulo) {
        if (articulo == null || articulo.getTaxones() == null || articulo.getTaxones().isEmpty()) {
            return List.of();
        }
        return articulo.getTaxones().stream()
                .filter(ta -> ta.getTipo() == TipoVinculo.CLASIFICACION)
                .map(TaxonArticulo::getTaxon)
                .filter(Objects::nonNull)
                .map(Taxon::getNombre)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private void vincularSiNoExiste(Long articuloId, Long taxonId, TipoVinculo tipo) {
        // La UK es (articulo_id, taxon_id) sin tipo: subcategoría y clasificación
        // con el mismo nombre (p.ej. CASUAL / Casual) comparten taxón y no pueden duplicarse.
        if (taxonArticuloRepository.existsByArticuloIdAndTaxonId(articuloId, taxonId)) {
            return;
        }
        TaxonArticulo link = new TaxonArticulo();
        link.setArticuloId(articuloId);
        link.setTaxonId(taxonId);
        link.setTipo(tipo);
        taxonArticuloRepository.save(link);
    }
}
