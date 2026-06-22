package com.vida.apirest.servicies;

import com.vida.apirest.model.articulo.Categoria;
import com.vida.apirest.model.articulo.Color;
import com.vida.apirest.model.articulo.Genero;
import com.vida.apirest.model.articulo.Marca;
import com.vida.apirest.model.articulo.Talle;
import com.vida.apirest.model.articulo.Taxon;
import com.vida.apirest.repositories.CategoriaRepository;
import com.vida.apirest.repositories.ColorRepository;
import com.vida.apirest.repositories.GeneroRepository;
import com.vida.apirest.repositories.MarcaRepository;
import com.vida.apirest.repositories.TalleRepository;
import com.vida.apirest.repositories.TaxonRepository;
import com.vida.apirest.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogoResolverService {

    private final MarcaRepository marcaRepository;
    private final CategoriaRepository categoriaRepository;
    private final GeneroRepository generoRepository;
    private final ColorRepository colorRepository;
    private final TalleRepository talleRepository;
    private final TaxonRepository taxonRepository;

    @Transactional
    public Marca findOrCreateMarca(String nombre) {
        String normalizado = ValidationUtils.requireNonBlank(nombre, "La marca es obligatoria");
        return marcaRepository.findByNombre(normalizado)
                .orElseGet(() -> {
                    Marca marca = new Marca();
                    marca.setNombre(normalizado);
                    return marcaRepository.save(marca);
                });
    }

    @Transactional
    public Categoria findOrCreateCategoria(String nombre) {
        String normalizado = ValidationUtils.requireNonBlank(nombre, "La categoría es obligatoria");
        return categoriaRepository.findByNombre(normalizado)
                .orElseGet(() -> {
                    Categoria categoria = new Categoria();
                    categoria.setNombre(normalizado);
                    return categoriaRepository.save(categoria);
                });
    }

    @Transactional
    public Genero findOrCreateGenero(String nombre) {
        String normalizado = ValidationUtils.requireNonBlank(nombre, "El género es obligatorio");
        return generoRepository.findByNombre(normalizado)
                .orElseGet(() -> {
                    Genero genero = new Genero();
                    genero.setNombre(normalizado);
                    return generoRepository.save(genero);
                });
    }

    @Transactional
    public Color findOrCreateColor(String nombre) {
        String normalizado = ValidationUtils.requireNonBlank(nombre, "El color es obligatorio");
        return colorRepository.findByNombre(normalizado)
                .orElseGet(() -> {
                    Color color = new Color();
                    color.setNombre(normalizado);
                    return colorRepository.save(color);
                });
    }

    @Transactional
    public Taxon findOrCreateClasificacion(String nombre) {
        return findOrCreateSubCategoria(nombre);
    }

    @Transactional
    public Taxon findOrCreateSubCategoria(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        String normalizado = nombre.trim();
        return taxonRepository.findByNombreIgnoreCase(normalizado)
                .orElseGet(() -> {
                    Taxon taxon = new Taxon();
                    taxon.setNombre(normalizado);
                    taxon.setNivel(1);
                    taxon.setActivo(true);
                    return taxonRepository.save(taxon);
                });
    }

    @Transactional
    public Talle findOrCreateTalle(String paisCodigo, String numero) {
        Talle.Pais pais = parsePais(paisCodigo);
        String talleNumero = ValidationUtils.requireNonBlank(numero, "El número de talle es obligatorio");
        return talleRepository.findByPaisAndNumero(pais, talleNumero)
                .orElseGet(() -> {
                    Talle talle = new Talle();
                    talle.setPais(pais);
                    talle.setNumero(talleNumero);
                    talle.setDescripcion("Talle " + talleNumero + " - " + pais);
                    return talleRepository.save(talle);
                });
    }

    public Talle.Pais parsePais(String paisCodigo) {
        try {
            return Talle.Pais.valueOf(ValidationUtils.requireNonBlank(
                    paisCodigo, "El país de talle es obligatorio").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("País de talle inválido. Valores válidos: AR, UK, BR, US, EU");
        }
    }
}
