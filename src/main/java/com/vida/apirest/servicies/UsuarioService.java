package com.vida.apirest.servicies;

import com.vida.apirest.dto.usuario.*;
import com.vida.apirest.dto.usuario.mapper.UsuarioMapper;
import com.vida.apirest.model.auth.Role;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.model.auth.UsuarioHasRoles;
import com.vida.apirest.repositories.RoleRepository;
import com.vida.apirest.repositories.UsuarioHasRoleRepository;
import com.vida.apirest.repositories.UsuarioRepository;
import com.vida.apirest.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioHasRoleRepository usuarioHasRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioMapper usuarioMapper;


    @Transactional
    public LoginResponse create(CreateUsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email)) {
            throw new RuntimeException("El correo ya esta en uso");


        }
        Usuario usuario = new Usuario();
        usuario.setUsuario(request.usuario);
        usuario.setEmail(request.email);
        usuario.setCelular(request.celular);

        String encryptedPassword = passwordEncoder.encode(request.password);
        usuario.setPassword(encryptedPassword);

        Usuario savedUser = usuarioRepository.save(usuario);
        Role clientRole = roleRepository.findByNombre("CLIENTE").orElseThrow(
                () -> new RuntimeException("El rol cliente no existe")
        );

        UsuarioHasRoles usuarioHasRoles = new UsuarioHasRoles(savedUser, clientRole);
        usuarioHasRoleRepository.save(usuarioHasRoles);


        String token = jwtUtil.generatToken(usuario);
        List<Role> roles = roleRepository.findAllByUsuariosHasRoles_Usuario_Id(savedUser.getId());

        LoginResponse response = new LoginResponse();
        response.setToken("Bearer " + token);
        response.setUsuario(usuarioMapper.toUsuarioResponse(usuario, roles));


        return response;


    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail()).orElseThrow(() -> new RuntimeException("El email o password no son validos"));
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new RuntimeException("El password no es valido");
        }
        String token = jwtUtil.generatToken(usuario);
        List<Role> roles = roleRepository.findAllByUsuariosHasRoles_Usuario_Id(usuario.getId());

        LoginResponse response = new LoginResponse();
        response.setToken("Bearer " + token);
        response.setUsuario(usuarioMapper.toUsuarioResponse(usuario, roles));
        return response;


    }

    @Transactional
    public UsuarioResponse findById(Long id) {

        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> new RuntimeException("El email o password no son validos"));
        List<Role> roles = roleRepository.findAllByUsuariosHasRoles_Usuario_Id(usuario.getId());


        return usuarioMapper.toUsuarioResponse(usuario, roles);
    }

    @Transactional
    public UsuarioResponse updateUsuarioConImagen(Long id, UpdateUsuarioRequest request) throws IOException {

        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> new RuntimeException("El email o password no son validos"));

        if (request.getCelular() != null) {
            usuario.setCelular(request.getCelular());
        }

        if (request.getFile() != null && !request.getFile().isEmpty()) {
            String uploadDir = "upload/usuario/" + usuario.getId();
            String filename = request.getFile().getOriginalFilename();
            String filePath = Paths.get(uploadDir, filename).toString();

            Files.createDirectories(Paths.get(uploadDir));
            Files.copy(request.getFile().getInputStream(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
            usuario.setImage("/" + filePath.replace("\\", "/"));
        }

        usuarioRepository.save(usuario);


        List<Role> roles = roleRepository.findAllByUsuariosHasRoles_Usuario_Id(usuario.getId());

        return usuarioMapper.toUsuarioResponse(usuario, roles);
    }

}
