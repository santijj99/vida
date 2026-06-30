package com.vida.apirest.servicies;

import com.vida.apirest.dto.empresa.EmpresaCreateRequest;
import com.vida.apirest.dto.empresa.EmpresaResponse;
import com.vida.apirest.dto.empresa.EmpresaUpdateRequest;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.repositories.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    @Transactional
    public EmpresaResponse create(EmpresaCreateRequest request) {
        Empresa empresa = new Empresa();
        empresa.setNombre(request.getNombre());
        empresa.setCodigo(request.getCodigo());
        empresa.setCuit(request.getCuit());
        empresa.setRazonSocial(request.getRazonSocial());
        empresa.setDomicilio(request.getDomicilio());
        empresa.setCiudad(request.getCiudad());
        empresa.setEstado(Empresa.EstadoEmpresa.ACTIVA);
        return toResponse(empresaRepository.save(empresa));
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponse> findAll() {
        return empresaRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmpresaResponse findById(Long id) {
        return toResponse(empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada con ID: " + id)));
    }

    @Transactional
    public EmpresaResponse update(Long id, EmpresaUpdateRequest request) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada con ID: " + id));

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            empresa.setNombre(request.getNombre().trim());
        }
        if (request.getCodigo() != null && !request.getCodigo().isBlank()) {
            String codigo = request.getCodigo().trim();
            empresaRepository.findByCodigo(codigo)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new RuntimeException("Ya existe otra empresa con el código: " + codigo);
                    });
            empresa.setCodigo(codigo);
        }
        if (request.getCuit() != null) {
            String cuit = request.getCuit().trim();
            if (!cuit.isBlank()) {
                empresaRepository.findByCuit(cuit)
                        .filter(existing -> !existing.getId().equals(id))
                        .ifPresent(existing -> {
                            throw new RuntimeException("Ya existe otra empresa con el CUIT: " + cuit);
                        });
            }
            empresa.setCuit(cuit.isBlank() ? null : cuit);
        }
        if (request.getRazonSocial() != null) {
            empresa.setRazonSocial(request.getRazonSocial().trim());
        }
        if (request.getDomicilio() != null) {
            empresa.setDomicilio(request.getDomicilio().trim());
        }
        if (request.getCiudad() != null) {
            empresa.setCiudad(request.getCiudad().trim());
        }
        if (request.getProvincia() != null) {
            empresa.setProvincia(request.getProvincia().trim());
        }
        if (request.getEstado() != null && !request.getEstado().isBlank()) {
            empresa.setEstado(Empresa.EstadoEmpresa.valueOf(request.getEstado().trim().toUpperCase()));
        }

        return toResponse(empresaRepository.save(empresa));
    }

    private EmpresaResponse toResponse(Empresa empresa) {
        EmpresaResponse response = new EmpresaResponse();
        response.setId(empresa.getId());
        response.setNombre(empresa.getNombre());
        response.setCodigo(empresa.getCodigo());
        response.setCuit(empresa.getCuit());
        response.setRazonSocial(empresa.getRazonSocial());
        response.setDomicilio(empresa.getDomicilio());
        response.setCiudad(empresa.getCiudad());
        response.setProvincia(empresa.getProvincia());
        response.setEstado(empresa.getEstado() != null ? empresa.getEstado().name() : null);
        return response;
    }
}
