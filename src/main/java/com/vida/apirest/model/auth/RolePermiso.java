package com.vida.apirest.model.auth;

import com.vida.apirest.model.auth.id.RolePermisoId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Entity
@Table(name = "role_permiso")
public class RolePermiso {

    @EmbeddedId
    private RolePermisoId id = new RolePermisoId();

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne
    @MapsId("permisoId")
    @JoinColumn(name = "permiso_id")
    private Permiso permiso;

    public RolePermiso(Role role, Permiso permiso) {
        this.role = role;
        this.permiso = permiso;
        if (role != null && role.getId() != null && permiso != null && permiso.getId() != null) {
            this.id = new RolePermisoId(role.getId(), permiso.getId());
        }
    }
}
