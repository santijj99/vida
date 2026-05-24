package com.vida.apirest.servicies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vida.apirest.dto.empleado.CreateEmpleadoRequest;
import com.vida.apirest.dto.empleado.EmpleadoResponse;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.model.persona.Empleado;
import com.vida.apirest.repositories.EmpleadoRepository;
import com.vida.apirest.repositories.RoleRepository;
import com.vida.apirest.model.auth.Role;
import com.vida.apirest.model.auth.UsuarioHasRoles;
import com.vida.apirest.repositories.UsuarioHasRoleRepository;
import com.vida.apirest.repositories.UsuarioRepository;

@Service
public class EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UsuarioHasRoleRepository usuarioHasRoleRepository;

    @Transactional
    public List<EmpleadoResponse> findAll() {
        return empleadoRepository.findAll().stream().map(this::toEmpleadoResponse).collect(Collectors.toList());
    }

    @Transactional
    public EmpleadoResponse findById(Long id) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
        return toEmpleadoResponse(empleado);
    }

    @Transactional
    public Empleado create(CreateEmpleadoRequest request) throws IOException {
        Empleado empleado = new Empleado();
        empleado.setNombre(request.getNombre());
        empleado.setApellido(request.getApellido());
        empleado.setDni(request.getDni());

        Empleado empleadoSaved = empleadoRepository.save(empleado);

        if (request.getFile() != null && !request.getFile().isEmpty()) {
            String uploadDir = "uploads/empleado/" + empleadoSaved.getId();
            String fileName = getPerfilFileName(request.getFile().getOriginalFilename());
            String filePath = Paths.get(uploadDir, fileName).toString();

            Files.createDirectories(Paths.get(uploadDir));
            Files.copy(request.getFile().getInputStream(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
            empleadoSaved.setImage("/" + filePath.replace("\\", "/"));
            empleadoRepository.save(empleadoSaved);

        }

        return empleadoSaved;
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
        empleado.setNombre(request.getNombre());
        empleado.setApellido(request.getApellido());
        empleado.setDni(request.getDni());

        Empleado empleadoSaved = empleadoRepository.save(empleado);

        if (request.getFile() != null && !request.getFile().isEmpty()) {
            String uploadDir = "uploads/empleado/" + empleadoSaved.getId();
            String fileName = getPerfilFileName(request.getFile().getOriginalFilename());
            String filePath = Paths.get(uploadDir, fileName).toString();
            Files.createDirectories(Paths.get(uploadDir));
            Files.copy(request.getFile().getInputStream(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
            empleadoSaved.setImage("/" + filePath.replace("\\", "/"));
            empleadoRepository.save(empleadoSaved);
        }

        if (request.getActivo() != null) {
            empleado.setActivo(request.getActivo());
        }

        // --- NUEVA LÓGICA DE VINCULACIÓN Y ROLES ---
        if (request.getUsuarioId() != null) {
            Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                    .orElseThrow(() -> new RuntimeException("Usuario para empleado no encontrado"));
            empleado.setUsuario(usuario);

            // 1. Buscamos el rol EMPLEADO en la base de datos
            Role empleadoRole = roleRepository.findByNombre("EMPLEADO")
                    .orElseThrow(() -> new RuntimeException("El rol EMPLEADO no existe en la BD"));

            // 2. Verificamos si el usuario ya tiene este rol asignado
            List<Role> userRoles = roleRepository.findAllByUsuariosHasRoles_Usuario_Id(usuario.getId());
            boolean hasRoleEmpleado = userRoles.stream().anyMatch(r -> r.getNombre().equals("EMPLEADO"));

            // 3. Si no lo tiene, se lo agregamos automáticamente
            if (!hasRoleEmpleado) {
                UsuarioHasRoles nuevoRol = new UsuarioHasRoles(usuario, empleadoRole);
                usuarioHasRoleRepository.save(nuevoRol);
            }

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
            response.setRoles(rolesBD.stream().map(Role::getNombre).collect(Collectors.toList()));
        }

        return response;
    }

    private String getPerfilFileName(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return "perfil" + originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "perfil";
    }
}
