package com.vida.apirest.servicies;

import com.vida.apirest.dto.almacen.DepositoCreateRequest;
import com.vida.apirest.dto.almacen.DepositoResponse;
import com.vida.apirest.model.almacen.Deposito;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.repositories.DepositoRepository;
import com.vida.apirest.repositories.SucursalRepository;
import com.vida.apirest.utils.EntityLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepositoService {

    private final DepositoRepository depositoRepository;
    private final SucursalRepository sucursalRepository;

    @Transactional
    public DepositoResponse create(DepositoCreateRequest request) {
        Sucursal sucursal = EntityLookup.require(
                sucursalRepository.findById(request.getSucursalId()),
                "Sucursal no encontrada con ID: " + request.getSucursalId());

        Deposito deposito = new Deposito();
        deposito.setSucursal(sucursal);
        deposito.setNombre(request.getNombre());
        deposito.setCodigo(request.getCodigo());
        deposito.setUbicacion(request.getUbicacion());
        deposito.setDescripcion(request.getDescripcion());
        deposito.setTipo(parseTipo(request.getTipo()));
        return toResponse(depositoRepository.save(deposito));
    }

    @Transactional
    public DepositoResponse update(Long id, DepositoCreateRequest request) {
        Deposito deposito = EntityLookup.require(
                depositoRepository.findById(id),
                "Depósito no encontrado con ID: " + id);

        if (request.getSucursalId() != null
                && (deposito.getSucursal() == null
                || !request.getSucursalId().equals(deposito.getSucursal().getId()))) {
            Sucursal sucursal = EntityLookup.require(
                    sucursalRepository.findById(request.getSucursalId()),
                    "Sucursal no encontrada con ID: " + request.getSucursalId());
            deposito.setSucursal(sucursal);
        }
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            deposito.setNombre(request.getNombre().trim());
        }
        if (request.getCodigo() != null && !request.getCodigo().isBlank()) {
            deposito.setCodigo(request.getCodigo().trim());
        }
        if (request.getUbicacion() != null) {
            deposito.setUbicacion(request.getUbicacion().trim());
        }
        if (request.getDescripcion() != null) {
            deposito.setDescripcion(request.getDescripcion().trim());
        }
        if (request.getTipo() != null && !request.getTipo().isBlank()) {
            deposito.setTipo(parseTipo(request.getTipo()));
        }
        return toResponse(depositoRepository.save(deposito));
    }

    @Transactional(readOnly = true)
    public List<DepositoResponse> findAll(Long sucursalId) {
        List<Deposito> depositos = sucursalId != null
                ? depositoRepository.findBySucursalIdOrderByNombreAsc(sucursalId)
                : depositoRepository.findAll();
        return depositos.stream().map(this::toResponse).toList();
    }

    private Deposito.Tipo parseTipo(String tipo) {
        try {
            return Deposito.Tipo.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RuntimeException(
                    "Tipo de depósito inválido. Valores: PRINCIPAL, SECUNDARIO, AUXILIAR, DISTRIBUCION");
        }
    }

    private DepositoResponse toResponse(Deposito deposito) {
        DepositoResponse response = new DepositoResponse();
        response.setId(deposito.getId());
        if (deposito.getSucursal() != null) {
            response.setSucursalId(deposito.getSucursal().getId());
            response.setSucursalNombre(deposito.getSucursal().getNombre());
        }
        response.setNombre(deposito.getNombre());
        response.setCodigo(deposito.getCodigo());
        response.setUbicacion(deposito.getUbicacion());
        response.setDescripcion(deposito.getDescripcion());
        response.setTipo(deposito.getTipo() != null ? deposito.getTipo().name() : null);
        return response;
    }
}
