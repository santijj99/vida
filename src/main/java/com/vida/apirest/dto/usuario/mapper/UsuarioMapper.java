package com.vida.apirest.dto.usuario.mapper;

import com.vida.apirest.config.APIConfig;
import com.vida.apirest.dto.role.RoleDTO;
import com.vida.apirest.dto.usuario.UsuarioResponse;
import com.vida.apirest.model.auth.Role;
import com.vida.apirest.model.auth.Usuario;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UsuarioMapper {
    public UsuarioResponse toUsuarioResponse (Usuario usuario , List<Role> roles){

        List<RoleDTO> roleDTOS = roles.stream()
                .map(role -> new RoleDTO(role.getId(), role.getNombre(), role.getImage(), role.getRoute()))
                .toList();

        UsuarioResponse usuarioResponse = new UsuarioResponse();
        usuarioResponse.setId(usuario.getId());
        usuarioResponse.setUsuario(usuario.getUsuario());
        usuarioResponse.setCelular(usuario.getCelular());
        usuarioResponse.setEmail(usuario.getEmail());
        usuarioResponse.setRoles(roleDTOS);

        if (usuario.getImage()!=null){
            String imageUrl = APIConfig.BASE_URL + usuario.getImage();
            usuarioResponse.setImage(imageUrl);


        }

        usuarioResponse.setImage(usuario.getImage());

        return usuarioResponse;
    }
}
