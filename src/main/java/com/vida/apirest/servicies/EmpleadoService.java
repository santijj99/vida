package com.vida.apirest.servicies;

import com.vida.apirest.dto.empleado.CreateEmpleadoRequest;
import com.vida.apirest.dto.empleado.EmpleadoResponse;
import com.vida.apirest.model.persona.Empleado;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.repositories.EmpleadoRepository;
import com.vida.apirest.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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
        Empleado empleado = new  Empleado();
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
        empleadoRepository.delete(empleado);
    }

    private void mapRequestToEmpleado(CreateEmpleadoRequest request, Empleado empleado) throws IOException {
        empleado.setNombre(request.getNombre());
        empleado.setApellido(request.getApellido());
        empleado.setDni(request.getDni());

        Empleado empleadoSaved = empleadoRepository.save(empleado);
        if(request.getFile()!=null && !request.getFile().isEmpty()){
            String uploadDir = "uploads/empleado/"+ empleadoSaved.getId();
            String fileName = getPerfilFileName(request.getFile().getOriginalFilename());
            String filePath = Paths.get(uploadDir,fileName).toString();
            Files.createDirectories(Paths.get(uploadDir));
            Files.copy(request.getFile().getInputStream(),Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
            empleadoSaved.setImage("/"+filePath.replace("\\","/"));
            empleadoRepository.save(empleadoSaved);

        }


        if (request.getActivo() != null) {
            empleado.setActivo(request.getActivo());
        }
        if (request.getUsuarioId() != null) {
            Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
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
