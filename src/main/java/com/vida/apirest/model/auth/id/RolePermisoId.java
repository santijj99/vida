package com.vida.apirest.model.auth.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class RolePermisoId implements Serializable {

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "permiso_id")
    private Long permisoId;
}
