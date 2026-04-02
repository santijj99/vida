package com.vida.apirest.dto.usuario;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.vida.apirest.dto.role.RoleDTO;
import lombok.Data;

import java.util.List;

@Data
public class UsuarioResponse {
    public Long id;
    public String usuario;
    public String email;
    public String image;

    @JsonProperty("notification_token")
    public String notificationToken;

    public String celular;

    List<RoleDTO>roles;
}
