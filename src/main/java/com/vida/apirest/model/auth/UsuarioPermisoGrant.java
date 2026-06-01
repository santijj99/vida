package com.vida.apirest.model.auth;

import com.vida.apirest.model.auth.id.UsuarioPermisoId;
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
@Table(name = "usuario_permiso_grant")
public class UsuarioPermisoGrant {

    @EmbeddedId
    private UsuarioPermisoId id = new UsuarioPermisoId();

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne
    @MapsId("permisoId")
    @JoinColumn(name = "permiso_id")
    private Permiso permiso;

    public UsuarioPermisoGrant(Usuario usuario, Permiso permiso) {
        this.usuario = usuario;
        this.permiso = permiso;
        if (usuario != null && usuario.getId() != null && permiso != null && permiso.getId() != null) {
            this.id = new UsuarioPermisoId(usuario.getId(), permiso.getId());
        }
    }
}
