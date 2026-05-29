package com.vida.apirest.dto.auth;

import com.vida.apirest.dto.role.RoleDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPermisosResponse {
    private Long usuarioId;
    private String usuario;
    private String email;
    private RoleDTO rolPrincipal;
    private List<RoleDTO> roles;
    private Set<String> permisosHeredados;
    private Set<String> permisosAdicionales;
    private Set<String> permisosDenegados;
    private Set<String> permisosEfectivos;
}
