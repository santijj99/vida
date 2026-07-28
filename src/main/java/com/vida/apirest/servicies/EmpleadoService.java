package com.vida.apirest.servicies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vida.apirest.dto.empleado.CreateEmpleadoRequest;
import com.vida.apirest.dto.empleado.EmpleadoResponse;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.model.persona.Empleado;
import com.vida.apirest.repositories.EmpleadoRepository;
import com.vida.apirest.repositories.RoleRepository;
import com.vida.apirest.repositories.UsuarioRepository;
import com.vida.apirest.repositories.UsuarioSucursalRepository;
import com.vida.apirest.model.auth.Role;
import com.vida.apirest.utils.FileUploadUtils;

@Service
public class EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UsuarioSucursalRepository usuarioSucursalRepository;

    @Transactional
    public List<EmpleadoResponse> findAll() {
        return empleadoRepository.findAll().stream().map(this::toEmpleadoResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EmpleadoResponse> findActivosParaVenta() {
        return empleadoRepository.findByActivoTrueOrderByApellidoAscNombreAsc()
                .stream()
                .map(this::toEmpleadoResponse)
                .toList();
    }

    @Transactional
    public EmpleadoResponse findById(Long id) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
        return toEmpleadoResponse(empleado);
    }

    @Transactional
    public EmpleadoResponse create(CreateEmpleadoRequest request) throws IOException {
        Empleado empleado = new Empleado();
        empleado.setNombre(request.getNombre());
        empleado.setApellido(request.getApellido());
        empleado.setDni(request.getDni());

        if (request.getActivo() != null) {
            empleado.setActivo(request.getActivo());
        }

        Empleado empleadoSaved = empleadoRepository.save(empleado);
        empleadoSaved = guardarImagenSiExiste(request, empleadoSaved);
        vincularUsuario(empleadoSaved, request.getUsuarioId());
        empleadoSaved = empleadoRepository.save(empleadoSaved);

        return toEmpleadoResponse(empleadoSaved);
    }

    @Transactional
    public EmpleadoResponse update(Long id, CreateEmpleadoRequest request) throws IOException {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        mapRequestToEmpleado(request, empleado);

        Empleado updated = empleadoRepository.save(empleado);
        return toEmpleadoResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
        empleado.setActivo(false);
        if (empleado.getUsuario() != null) {
            empleado.getUsuario().setActivo(false);
            usuarioRepository.save(empleado.getUsuario());
        }

        empleadoRepository.save(empleado);
    }

    private void mapRequestToEmpleado(CreateEmpleadoRequest request, Empleado empleado) throws IOException {
        if (request.getNombre() != null) {
            empleado.setNombre(request.getNombre());
        }
        if (request.getApellido() != null) {
            empleado.setApellido(request.getApellido());
        }
        if (request.getDni() != null) {
            empleado.setDni(request.getDni());
        }

        if (request.getActivo() != null) {
            empleado.setActivo(request.getActivo());
        }

        guardarImagenSiExiste(request, empleado);
        vincularUsuario(empleado, request.getUsuarioId());
    }

    private Empleado guardarImagenSiExiste(CreateEmpleadoRequest request, Empleado empleado) throws IOException {
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            String uploadDir = "uploads/empleado/" + empleado.getId();
            String fileName = FileUploadUtils.safeProfileFileName(request.getFile().getOriginalFilename());
            String filePath = Paths.get(uploadDir, fileName).toString();

            Files.createDirectories(Paths.get(uploadDir));
            Files.copy(request.getFile().getInputStream(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
            empleado.setImage("/" + filePath.replace("\\", "/"));
            return empleadoRepository.save(empleado);
        }
        return empleado;
    }

    private void vincularUsuario(Empleado empleado, Long usuarioId) {
        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RuntimeException("Usuario para empleado no encontrado"));
            empleado.setUsuario(usuario);
        } else {
            empleado.setUsuario(null);
        }
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
            response.setRolPrincipal(resolverRolPrincipal(nombresRoles));
            response.setSucursales(usuarioSucursalRepository.findSucursalNombresByUsuarioId(empleado.getUsuario().getId()));
        } else {
            response.setSucursales(List.of());
        }

        return response;
    }

    private String resolverRolPrincipal(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        if (roles.contains("ADMINISTRADOR")) {
            return "ADMINISTRADOR";
        }
        return roles.get(0);
    }
}
