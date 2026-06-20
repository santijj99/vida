package com.vida.apirest.servicies;

import com.vida.apirest.dto.empresa.EmpresaCreateRequest;
import com.vida.apirest.dto.empresa.EmpresaResponse;
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

    private EmpresaResponse toResponse(Empresa empresa) {
        EmpresaResponse response = new EmpresaResponse();
        response.setId(empresa.getId());
        response.setNombre(empresa.getNombre());
        response.setCodigo(empresa.getCodigo());
        response.setCuit(empresa.getCuit());
        response.setRazonSocial(empresa.getRazonSocial());
        response.setDomicilio(empresa.getDomicilio());
        response.setCiudad(empresa.getCiudad());
        response.setEstado(empresa.getEstado() != null ? empresa.getEstado().name() : null);
        return response;
    }
}
