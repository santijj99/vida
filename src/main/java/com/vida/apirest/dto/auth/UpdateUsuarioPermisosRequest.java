package com.vida.apirest.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUsuarioPermisosRequest {
    private Long rolPrincipalId;
    private List<Long> permisosAdicionalesIds;
    private List<Long> permisosDenegadosIds;
}
