package com.vida.apirest.servicies;

import com.vida.apirest.dto.cliente.*;
import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.model.persona.Contacto;
import com.vida.apirest.model.persona.Direccion;
import com.vida.apirest.repositories.ClienteRepository;
import com.vida.apirest.repositories.DireccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private DireccionRepository direccionRepository;

    @Transactional
    public List<ClienteResponse> findAll() {
        return clienteRepository.findAll().stream().map(this::toClienteResponse).collect(Collectors.toList());
    }

    @Transactional
    public ClienteResponse findById(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return toClienteResponse(cliente);
    }

    @Transactional
    public ClienteResponse createClienteOnly(CreateClienteSimpleRequest request) {
        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setDni(request.getDni());
        
        // Agregar dirección si se proporciona
        if (request.getDireccionId() != null) {
            Direccion direccion = direccionRepository.findById(request.getDireccionId())
                    .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
            cliente.setDireccion(direccion);
        }
        
        // Sin garante ni contactos
        cliente.setGarante(null);
        
        Cliente saved = clienteRepository.save(cliente);
        return toClienteResponse(saved);
    }

    @Transactional
    public ClienteResponse createClienteWithGaranteAndContacto(CreateClienteWithGaranteAndContactoRequest request) {
        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setDni(request.getDni());

        // Agregar dirección si se proporciona
        if (request.getDireccionId() != null) {
            Direccion direccion = direccionRepository.findById(request.getDireccionId())
                    .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
            cliente.setDireccion(direccion);
        }

        // Agregar garante si se proporciona
        if (request.getGaranteId() != null) {
            Cliente garante = clienteRepository.findById(request.getGaranteId())
                    .orElseThrow(() -> new RuntimeException("Garante no encontrado"));
            cliente.setGarante(garante);
        } else {
            cliente.setGarante(null);
        }

        // Agregar contactos si se proporcionan
        if (request.getContactos() != null && !request.getContactos().isEmpty()) {
            for (ContactoRequest contactoRequest : request.getContactos()) {
                Contacto contacto = new Contacto();
                contacto.setNombre(contactoRequest.getNombre());
                contacto.setApellido(contactoRequest.getApellido());
                contacto.setEmail(contactoRequest.getEmail());
                contacto.setTelefono(contactoRequest.getTelefono());
                contacto.setCliente(cliente);
                cliente.getContactos().add(contacto);
            }
        }

        Cliente saved = clienteRepository.save(cliente);
        return toClienteResponse(saved);
    }

    @Transactional
    public ClienteResponse create(CreateClienteRequest request) {
        Cliente cliente = new Cliente();
        mapRequestToCliente(request, cliente);

        Cliente saved = clienteRepository.save(cliente);
        return toClienteResponse(saved);
    }

    @Transactional
    public ClienteResponse update(Long id, CreateClienteRequest request) {
        Cliente clienteExistente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        mapRequestToCliente(request, clienteExistente);

        Cliente updated = clienteRepository.save(clienteExistente);
        return toClienteResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        clienteRepository.delete(cliente);
    }

    private void mapRequestToCliente(CreateClienteRequest request, Cliente cliente) {
        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setDni(request.getDni());

        // Agregar dirección si se proporciona
        if (request.getDireccionId() != null) {
            Direccion direccion = direccionRepository.findById(request.getDireccionId())
                    .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
            cliente.setDireccion(direccion);
        }

        if (request.getGaranteId() != null) {
            Cliente garante = clienteRepository.findById(request.getGaranteId())
                    .orElseThrow(() -> new RuntimeException("Garante no encontrado"));
            cliente.setGarante(garante);
        } else {
            cliente.setGarante(null);
        }

        if (request.getContactos() != null) {
            cliente.getContactos().clear();
            for (ContactoRequest contactoRequest : request.getContactos()) {
                Contacto contacto = new Contacto();
                contacto.setNombre(contactoRequest.getNombre());
                contacto.setApellido(contactoRequest.getApellido());
                contacto.setEmail(contactoRequest.getEmail());
                contacto.setTelefono(contactoRequest.getTelefono());
                contacto.setCliente(cliente);
                cliente.getContactos().add(contacto);
            }
        }
    }

    private ClienteResponse toClienteResponse(Cliente cliente) {
        ClienteResponse response = new ClienteResponse();
        response.setId(cliente.getId());
        response.setNombre(cliente.getNombre());
        response.setApellido(cliente.getApellido());
        response.setDni(cliente.getDni());

        // Agregar dirección
        if (cliente.getDireccion() != null) {
            response.setDireccionId(cliente.getDireccion().getId());
            DireccionResponse direccionResponse = new DireccionResponse();
            direccionResponse.setId(cliente.getDireccion().getId());
            direccionResponse.setPais(cliente.getDireccion().getPais());
            direccionResponse.setProvincia(cliente.getDireccion().getProvincia());
            direccionResponse.setLocalidad(cliente.getDireccion().getLocalidad());
            direccionResponse.setBarrio(cliente.getDireccion().getBarrio());
            direccionResponse.setCalle(cliente.getDireccion().getCalle());
            direccionResponse.setNumero(cliente.getDireccion().getNumero());
            direccionResponse.setObservacion(cliente.getDireccion().getObservacion());
            response.setDireccion(direccionResponse);
        }

        if (cliente.getGarante() != null) {
            response.setGaranteId(cliente.getGarante().getId());
            response.setGaranteNombre(cliente.getGarante().getNombre() + " " + cliente.getGarante().getApellido());
        }

        List<ContactoResponse> contactos = cliente.getContactos().stream().map(c -> {
            ContactoResponse contactoResponse = new ContactoResponse();
            contactoResponse.setId(c.getId());
            contactoResponse.setNombre(c.getNombre());
            contactoResponse.setApellido(c.getApellido());
            contactoResponse.setEmail(c.getEmail());
            contactoResponse.setTelefono(c.getTelefono());
            return contactoResponse;
        }).collect(Collectors.toList());

        response.setContactos(contactos);
        return response;
    }
}
