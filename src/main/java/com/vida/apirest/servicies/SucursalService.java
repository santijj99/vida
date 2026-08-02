package com.vida.apirest.servicies;

import com.vida.apirest.dto.almacen.SucursalCreateRequest;
import com.vida.apirest.dto.almacen.SucursalResponse;
import com.vida.apirest.dto.empleado.EmpleadoResponse;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.auth.Role;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.model.auth.UsuarioSucursal;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.model.persona.Empleado;
import com.vida.apirest.repositories.EmpleadoRepository;
import com.vida.apirest.repositories.EmpresaRepository;
import com.vida.apirest.repositories.RoleRepository;
import com.vida.apirest.repositories.SucursalRepository;
import com.vida.apirest.repositories.UsuarioSucursalRepository;
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
    private final EmpleadoRepository empleadoRepository;
    private final UsuarioSucursalRepository usuarioSucursalRepository;
    private final RoleRepository roleRepository;

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

    @Transactional
    public SucursalResponse update(Long id, SucursalCreateRequest request) {
        Sucursal sucursal = requireSucursal(id);
        if (request.getEmpresaId() != null
                && (sucursal.getEmpresa() == null
                || !request.getEmpresaId().equals(sucursal.getEmpresa().getId()))) {
            Empresa empresa = EntityLookup.require(
                    empresaRepository.findById(request.getEmpresaId()),
                    "Empresa no encontrada con ID: " + request.getEmpresaId());
            sucursal.setEmpresa(empresa);
        }
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            sucursal.setNombre(request.getNombre().trim());
        }
        if (request.getCodigo() != null && !request.getCodigo().isBlank()) {
            sucursal.setCodigo(request.getCodigo().trim());
        }
        if (request.getDomicilio() != null) {
            sucursal.setDomicilio(request.getDomicilio().trim());
        }
        if (request.getCiudad() != null) {
            sucursal.setCiudad(request.getCiudad().trim());
        }
        if (request.getProvincia() != null) {
            sucursal.setProvincia(request.getProvincia().trim());
        }
        return toResponse(sucursalRepository.save(sucursal));
    }

    @Transactional(readOnly = true)
    public List<SucursalResponse> findAll() {
        return sucursalRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EmpleadoResponse> listarEmpleados(Long sucursalId) {
        requireSucursal(sucursalId);
        return usuarioSucursalRepository.findEmpleadosBySucursalId(sucursalId).stream()
                .map(this::toEmpleadoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmpleadoResponse> listarEmpleadosDisponibles(Long sucursalId) {
        requireSucursal(sucursalId);
        return empleadoRepository.findDisponiblesParaSucursal(sucursalId).stream()
                .map(this::toEmpleadoResponse)
                .toList();
    }

    @Transactional
    public EmpleadoResponse asignarEmpleado(Long sucursalId, Long empleadoId) {
        Sucursal sucursal = requireSucursal(sucursalId);
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + empleadoId));
        Usuario usuario = empleado.getUsuario();
        if (usuario == null) {
            throw new RuntimeException("El empleado no tiene usuario vinculado; no se puede asignar a la sucursal");
        }
        if (usuarioSucursalRepository.existsByUsuario_IdAndSucursal_Id(usuario.getId(), sucursalId)) {
            return toEmpleadoResponse(empleado);
        }
        usuarioSucursalRepository.save(new UsuarioSucursal(usuario, sucursal));
        return toEmpleadoResponse(empleado);
    }

    @Transactional
    public void quitarEmpleado(Long sucursalId, Long empleadoId) {
        requireSucursal(sucursalId);
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + empleadoId));
        if (empleado.getUsuario() == null) {
            throw new RuntimeException("El empleado no tiene usuario vinculado");
        }
        usuarioSucursalRepository.deleteByUsuario_IdAndSucursal_Id(empleado.getUsuario().getId(), sucursalId);
    }

    private Sucursal requireSucursal(Long sucursalId) {
        return EntityLookup.require(
                sucursalRepository.findById(sucursalId),
                "Sucursal no encontrada con ID: " + sucursalId);
    }

    private EmpleadoResponse toEmpleadoResponse(Empleado empleado) {
        EmpleadoResponse response = new EmpleadoResponse();
        response.setId(empleado.getId());
        response.setNombre(empleado.getNombre());
        response.setApellido(empleado.getApellido());
        response.setDni(empleado.getDni());
        response.setImage(empleado.getImage());
        response.setActivo(empleado.getActivo());
        if (empleado.getUsuario() != null) {
            response.setUsuarioId(empleado.getUsuario().getId());
            response.setCelular(empleado.getUsuario().getCelular());
            List<Role> rolesBD = roleRepository.findAllByUsuariosHasRoles_Usuario_Id(empleado.getUsuario().getId());
            List<String> nombresRoles = rolesBD.stream().map(Role::getNombre).toList();
            response.setRoles(nombresRoles);
            response.setRolPrincipal(nombresRoles.contains("ADMINISTRADOR")
                    ? "ADMINISTRADOR"
                    : (nombresRoles.isEmpty() ? null : nombresRoles.get(0)));
        }
        return response;
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
