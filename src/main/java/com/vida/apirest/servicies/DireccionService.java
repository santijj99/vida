package com.vida.apirest.servicies;

import com.vida.apirest.dto.cliente.DireccionRequest;
import com.vida.apirest.dto.cliente.DireccionResponse;
import com.vida.apirest.model.persona.Direccion;
import com.vida.apirest.repositories.DireccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DireccionService {

    @Autowired
    private DireccionRepository direccionRepository;

    @Transactional
    public List<DireccionResponse> findAll() {
        return direccionRepository.findAll().stream().map(this::toDireccionResponse).collect(Collectors.toList());
    }

    @Transactional
    public DireccionResponse findById(Long id) {
        Direccion direccion = direccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
        return toDireccionResponse(direccion);
    }

    @Transactional
    public DireccionResponse create(DireccionRequest request) {
        Direccion direccion = new Direccion();
        mapRequestToDireccion(request, direccion);

        Direccion saved = direccionRepository.save(direccion);
        return toDireccionResponse(saved);
    }

    @Transactional
    public DireccionResponse update(Long id, DireccionRequest request) {
        Direccion direccionExistente = direccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));

        mapRequestToDireccion(request, direccionExistente);

        Direccion updated = direccionRepository.save(direccionExistente);
        return toDireccionResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        Direccion direccion = direccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
        direccionRepository.delete(direccion);
    }

    private void mapRequestToDireccion(DireccionRequest request, Direccion direccion) {
        direccion.setPais(request.getPais());
        direccion.setProvincia(request.getProvincia());
        direccion.setLocalidad(request.getLocalidad());
        direccion.setBarrio(request.getBarrio());
        direccion.setCalle(request.getCalle());
        direccion.setNumero(request.getNumero());
        direccion.setObservacion(request.getObservacion());
    }

    private DireccionResponse toDireccionResponse(Direccion direccion) {
        DireccionResponse response = new DireccionResponse();
        response.setId(direccion.getId());
        response.setPais(direccion.getPais());
        response.setProvincia(direccion.getProvincia());
        response.setLocalidad(direccion.getLocalidad());
        response.setBarrio(direccion.getBarrio());
        response.setCalle(direccion.getCalle());
        response.setNumero(direccion.getNumero());
        response.setObservacion(direccion.getObservacion());
        return response;
    }
}
