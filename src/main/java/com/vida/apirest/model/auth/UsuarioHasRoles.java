package com.vida.apirest.model.auth;


import com.vida.apirest.model.auth.id.UsuarioRoleId;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@Table(name = "usuario_has_roles")
public class UsuarioHasRoles {

    @EmbeddedId
    private UsuarioRoleId id = new UsuarioRoleId();

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne
    @MapsId("usuarioId")
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "id_rol")
    private Role role;


    public UsuarioHasRoles(Usuario usuario, Role role) {
        this.usuario = usuario;
        this.role = role;
        if (usuario != null && usuario.getId() != null && role != null && role.getId() != null) {
            this.id = new UsuarioRoleId(usuario.getId(), role.getId());
        }
    }

    public UsuarioHasRoles() {
    }


}
