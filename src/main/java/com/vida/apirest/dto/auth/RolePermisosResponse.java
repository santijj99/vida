package com.vida.apirest.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermisosResponse {
    private Long roleId;
    private String roleNombre;
    private Set<String> permisos;
    private List<Long> permisoIds;
}
