package com.vida.apirest.servicies;

import com.vida.apirest.dto.finanzas.GastoCategoriaRequest;
import com.vida.apirest.dto.finanzas.GastoCategoriaResponse;
import com.vida.apirest.model.finanzas.GastoCategoria;
import com.vida.apirest.repositories.GastoCategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GastoCategoriaService {

    private final GastoCategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<GastoCategoriaResponse> listar(Boolean soloActivas) {
        List<GastoCategoria> items = Boolean.TRUE.equals(soloActivas)
                ? categoriaRepository.findByActivoTrueOrderByNombreAsc()
                : categoriaRepository.findAllByOrderByNombreAsc();
        return items.stream().map(this::map).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GastoCategoriaResponse findById(Long id) {
        return map(buscar(id));
    }

    @Transactional
    public GastoCategoriaResponse crear(GastoCategoriaRequest request) {
        validarNombre(request.getNombre());
        if (request.getCodigo() != null && !request.getCodigo().isBlank()) {
            categoriaRepository.findByCodigo(request.getCodigo().trim()).ifPresent(c -> {
                throw new RuntimeException("Ya existe una categoría con código: " + request.getCodigo());
            });
        }
        GastoCategoria categoria = new GastoCategoria();
        aplicarDatos(categoria, request);
        return map(categoriaRepository.save(categoria));
    }

    @Transactional
    public GastoCategoriaResponse actualizar(Long id, GastoCategoriaRequest request) {
        GastoCategoria categoria = buscar(id);
        validarNombre(request.getNombre());
        if (request.getCodigo() != null && !request.getCodigo().isBlank()) {
            categoriaRepository.findByCodigo(request.getCodigo().trim()).ifPresent(existente -> {
                if (!existente.getId().equals(id)) {
                    throw new RuntimeException("Ya existe una categoría con código: " + request.getCodigo());
                }
            });
        }
        aplicarDatos(categoria, request);
        return map(categoriaRepository.save(categoria));
    }

    @Transactional
    public void desactivar(Long id) {
        GastoCategoria categoria = buscar(id);
        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    GastoCategoria buscarEntidad(Long id) {
        return buscar(id);
    }

    private GastoCategoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría de gasto no encontrada: " + id));
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new RuntimeException("El nombre de la categoría es obligatorio");
        }
    }

    private void aplicarDatos(GastoCategoria categoria, GastoCategoriaRequest request) {
        categoria.setNombre(request.getNombre().trim());
        categoria.setCodigo(request.getCodigo() != null && !request.getCodigo().isBlank()
                ? request.getCodigo().trim() : null);
        categoria.setDescripcion(request.getDescripcion());
        if (request.getActivo() != null) {
            categoria.setActivo(request.getActivo());
        } else if (categoria.getActivo() == null) {
            categoria.setActivo(true);
        }
    }

    private GastoCategoriaResponse map(GastoCategoria categoria) {
        GastoCategoriaResponse r = new GastoCategoriaResponse();
        r.setId(categoria.getId());
        r.setNombre(categoria.getNombre());
        r.setCodigo(categoria.getCodigo());
        r.setDescripcion(categoria.getDescripcion());
        r.setActivo(categoria.getActivo());
        r.setCreatedAt(categoria.getCreatedAt());
        return r;
    }
}
