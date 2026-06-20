package com.vida.apirest.servicies;

import com.vida.apirest.dto.almacen.SucursalCreateRequest;
import com.vida.apirest.dto.almacen.SucursalResponse;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.repositories.EmpresaRepository;
import com.vida.apirest.repositories.SucursalRepository;
import com.vida.apirest.utils.EntityLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SucursalService {

    private final SucursalRepository sucursalRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional
    public SucursalResponse create(SucursalCreateRequest request) {
        Empresa empresa = EntityLookup.require(
                empresaRepository.findById(request.getEmpresaId()),
                "Empresa no encontrada con ID: " + request.getEmpresaId());

        Sucursal sucursal = new Sucursal();
        sucursal.setEmpresa(empresa);
        sucursal.setNombre(request.getNombre());
        sucursal.setCodigo(request.getCodigo());
        sucursal.setDomicilio(request.getDomicilio());
        sucursal.setCiudad(request.getCiudad());
        sucursal.setProvincia(request.getProvincia());
        sucursal.setEstado(Sucursal.EstadoSucursal.ACTIVA);
        return toResponse(sucursalRepository.save(sucursal));
    }

    @Transactional(readOnly = true)
    public List<SucursalResponse> findAll() {
        return sucursalRepository.findAll().stream().map(this::toResponse).toList();
    }

    private SucursalResponse toResponse(Sucursal sucursal) {
        SucursalResponse response = new SucursalResponse();
        response.setId(sucursal.getId());
        if (sucursal.getEmpresa() != null) {
            response.setEmpresaId(sucursal.getEmpresa().getId());
            response.setEmpresaNombre(sucursal.getEmpresa().getNombre());
        }
        response.setNombre(sucursal.getNombre());
        response.setCodigo(sucursal.getCodigo());
        response.setDomicilio(sucursal.getDomicilio());
        response.setCiudad(sucursal.getCiudad());
        response.setProvincia(sucursal.getProvincia());
        response.setEstado(sucursal.getEstado() != null ? sucursal.getEstado().name() : null);
        return response;
    }
}
