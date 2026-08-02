package com.vida.apirest.dto.usuario;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.vida.apirest.dto.auth.EffectivePermissions;
import com.vida.apirest.dto.role.RoleDTO;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class UsuarioResponse {
    public Long id;
    public String usuario;
    public String email;
    public String image;

    @JsonProperty("notification_token")
    public String notificationToken;

    public String celular;

    List<RoleDTO> roles;
    RoleDTO rolPrincipal;
    Set<String> permisosHeredados;
    Set<String> permisosAdicionales;
    Set<String> permisosDenegados;
    Set<String> permisosEfectivos;

    /** Sucursales operables: asignadas en usuario_sucursal; ADMINISTRADOR recibe todas las activas. */
    List<UsuarioSucursalDTO> sucursales;
}
