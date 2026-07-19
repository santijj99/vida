package com.vida.apirest.servicies;

import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.dto.proveedor.ProveedorRequest;
import com.vida.apirest.dto.proveedor.ProveedorResponse;
import com.vida.apirest.utils.PaginationUtils;
import com.vida.apirest.model.persona.Proveedor;
import com.vida.apirest.repositories.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public List<ProveedorResponse> findAllActivos() {
        return proveedorRepository.searchPage("", true, Pageable.unpaged()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProveedorResponse> findPage(String q, int page, int size, boolean soloActivos) {
        PaginationUtils.PageParams params = PaginationUtils.normalize(page, size);
        String query = PaginationUtils.normalizeQuery(q);
        Pageable pageable = PageRequest.of(
                params.page(),
                params.size(),
                Sort.by("razonSocial").ascending().and(Sort.by("nombre").ascending())
        );
        return PageResponse.from(
                proveedorRepository.searchPage(query, soloActivos, pageable).map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public ProveedorResponse findById(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        return toResponse(proveedor);
    }

    @Transactional
    public ProveedorResponse create(ProveedorRequest request) {
        validarRequest(request, null);
        Proveedor proveedor = new Proveedor();
        mapRequestToEntity(request, proveedor);
        proveedor.setActivo(request.getActivo() == null || request.getActivo());

        Proveedor saved = proveedorRepository.save(proveedor);
        if (saved.getCodigo() == null || saved.getCodigo().isBlank()) {
            saved.setCodigo("PROV-" + saved.getId());
            saved = proveedorRepository.save(saved);
        }
        return toResponse(saved);
    }

    @Transactional
    public ProveedorResponse update(Long id, ProveedorRequest request) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        validarRequest(request, id);
        mapRequestToEntity(request, proveedor);
        if (request.getActivo() != null) {
            proveedor.setActivo(request.getActivo());
        }
        return toResponse(proveedorRepository.save(proveedor));
    }

    @Transactional
    public void desactivar(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        proveedor.setActivo(false);
        proveedorRepository.save(proveedor);
    }

    private void validarRequest(ProveedorRequest request, Long idExcluir) {
        if (request.getRazonSocial() == null || request.getRazonSocial().isBlank()) {
            throw new RuntimeException("La razón social es obligatoria");
        }
        String codigo = request.getCodigo() == null ? "" : request.getCodigo().trim();
        if (!codigo.isEmpty()) {
            boolean duplicado = idExcluir == null
                    ? proveedorRepository.findByCodigo(codigo).isPresent()
                    : proveedorRepository.existsByCodigoAndIdNot(codigo, idExcluir);
            if (duplicado) {
                throw new RuntimeException("Ya existe un proveedor con el código " + codigo);
            }
        }
    }

    private void mapRequestToEntity(ProveedorRequest request, Proveedor proveedor) {
        proveedor.setCodigo(trimOrNull(request.getCodigo()));
        proveedor.setRazonSocial(request.getRazonSocial().trim());
        proveedor.setNombre(trimOrNull(request.getNombre()));
        proveedor.setCuitCuil(trimOrNull(request.getCuitCuil()));
        proveedor.setDomicilio(trimOrNull(request.getDomicilio()));
        proveedor.setCiudad(trimOrNull(request.getCiudad()));
        proveedor.setProvincia(trimOrNull(request.getProvincia()));
        proveedor.setPais(trimOrNull(request.getPais()));
        proveedor.setTelefono(trimOrNull(request.getTelefono()));
        proveedor.setEmail(trimOrNull(request.getEmail()));
    }

    private String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProveedorResponse toResponse(Proveedor proveedor) {
        ProveedorResponse response = new ProveedorResponse();
        response.setId(proveedor.getId());
        response.setCodigo(proveedor.getCodigo());
        response.setRazonSocial(proveedor.getRazonSocial());
        response.setNombre(proveedor.getNombre());
        response.setCuitCuil(proveedor.getCuitCuil());
        response.setDomicilio(proveedor.getDomicilio());
        response.setCiudad(proveedor.getCiudad());
        response.setProvincia(proveedor.getProvincia());
        response.setPais(proveedor.getPais());
        response.setTelefono(proveedor.getTelefono());
        response.setEmail(proveedor.getEmail());
        response.setActivo(proveedor.getActivo());
        response.setCreatedAt(proveedor.getCreatedAt());
        response.setUpdatedAt(proveedor.getUpdatedAt());
        return response;
    }
}
