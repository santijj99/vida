package com.vida.apirest.servicies;

import com.vida.apirest.dto.cliente.*;
import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.model.persona.Contacto;
import com.vida.apirest.model.persona.Direccion;
import com.vida.apirest.repositories.ClienteRepository;
import com.vida.apirest.repositories.DireccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
public class ClienteService {

    private static final int DEFAULT_PAGE_SIZE = 15;
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private DireccionRepository direccionRepository;

    @Transactional(readOnly = true)
    public List<ClienteResponse> findAll() {
        return clienteRepository.findAll().stream().map(this::toClienteResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<ClienteResponse> findPage(String q, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE));
        String query = q == null ? "" : q.trim();
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("apellido").ascending().and(Sort.by("nombre").ascending())
        );
        return PageResponse.from(
                clienteRepository.searchPage(query, pageable).map(this::toClienteResponse)
        );
    }

    @Transactional
    public ClienteResponse findById(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return toClienteResponse(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponse findByDni(String dni) {
        Cliente cliente = clienteRepository.findByDni(dni.trim())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con DNI: " + dni));
        return toClienteResponse(cliente);
    }

    @Transactional
    public ClienteResponse createClienteOnly(CreateClienteSimpleRequest request) {
        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setDni(request.getDni());
        
        cliente.setDireccion(resolverDireccion(request.getDireccionId(), null, null));
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

        cliente.setDireccion(resolverDireccion(request.getDireccionId(), null, null));
        cliente.setGarante(resolverGarante(request.getGaranteId(), null));
        agregarContactos(request.getContactos(), cliente, false);

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
        cliente.setTelefono(request.getTelefono());
        cliente.setTrabajo(request.getTrabajo());
        cliente.setDireccion(resolverDireccion(
                request.getDireccionId(), request.getDireccion(), cliente.getDireccion()));
        cliente.setGarante(resolverGarante(request.getGaranteId(), cliente.getId()));
        agregarContactos(request.getContactos(), cliente, true);
    }

    private Direccion resolverDireccion(
            Long direccionId, DireccionRequest direccionRequest, Direccion direccionActual) {
        if (tieneDatosDireccion(direccionRequest)) {
            if (direccionId != null) {
                Direccion direccion = direccionRepository.findById(direccionId)
                        .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
                mapDireccionRequest(direccionRequest, direccion);
                return direccionRepository.save(direccion);
            }
            if (direccionActual != null) {
                mapDireccionRequest(direccionRequest, direccionActual);
                return direccionRepository.save(direccionActual);
            }
            Direccion direccion = new Direccion();
            mapDireccionRequest(direccionRequest, direccion);
            return direccionRepository.save(direccion);
        }
        if (direccionId != null) {
            return direccionRepository.findById(direccionId)
                    .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
        }
        return direccionActual;
    }

    private Cliente resolverGarante(Long garanteId, Long clienteId) {
        if (garanteId == null) {
            return null;
        }
        if (clienteId != null && garanteId.equals(clienteId)) {
            throw new RuntimeException("Un cliente no puede ser garante de sí mismo");
        }
        return clienteRepository.findById(garanteId)
                .orElseThrow(() -> new RuntimeException("Garante no encontrado"));
    }

    private void agregarContactos(List<ContactoRequest> contactosRequest, Cliente cliente, boolean reemplazar) {
        if (contactosRequest == null) {
            return;
        }
        if (reemplazar) {
            cliente.getContactos().clear();
        }
        for (ContactoRequest contactoRequest : contactosRequest) {
            if (!tieneDatosContacto(contactoRequest)) {
                continue;
            }
            Contacto contacto = new Contacto();
            contacto.setNombre(contactoRequest.getNombre());
            contacto.setApellido(contactoRequest.getApellido());
            contacto.setEmail(contactoRequest.getEmail());
            contacto.setTelefono(contactoRequest.getTelefono());
            contacto.setCliente(cliente);
            cliente.getContactos().add(contacto);
        }
    }

    private void mapDireccionRequest(DireccionRequest request, Direccion direccion) {
        direccion.setPais(request.getPais());
        direccion.setProvincia(request.getProvincia());
        direccion.setLocalidad(request.getLocalidad());
        direccion.setBarrio(request.getBarrio());
        direccion.setCalle(request.getCalle());
        direccion.setNumero(request.getNumero());
        direccion.setObservacion(request.getObservacion());
    }

    private boolean tieneDatosDireccion(DireccionRequest request) {
        if (request == null) {
            return false;
        }
        return esTextoValido(request.getPais())
                || esTextoValido(request.getProvincia())
                || esTextoValido(request.getLocalidad())
                || esTextoValido(request.getBarrio())
                || esTextoValido(request.getCalle())
                || esTextoValido(request.getNumero())
                || esTextoValido(request.getObservacion());
    }

    private boolean tieneDatosContacto(ContactoRequest request) {
        return esTextoValido(request.getNombre())
                || esTextoValido(request.getApellido())
                || esTextoValido(request.getEmail())
                || esTextoValido(request.getTelefono());
    }

    private boolean esTextoValido(String valor) {
        return valor != null && !valor.isBlank();
    }

    private ClienteResponse toClienteResponse(Cliente cliente) {
        ClienteResponse response = new ClienteResponse();
        response.setId(cliente.getId());
        response.setNombre(cliente.getNombre());
        response.setApellido(cliente.getApellido());
        response.setDni(cliente.getDni());
        response.setTelefono(cliente.getTelefono());
        response.setTrabajo(cliente.getTrabajo());

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
