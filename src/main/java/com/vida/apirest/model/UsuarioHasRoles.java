package com.vida.apirest.model;


import com.vida.apirest.model.id.UsuarioRoleId;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "usuario_has_roles")
public class UsuarioHasRoles {

    @EmbeddedId
    private UsuarioRoleId id = new UsuarioRoleId();

    @ManyToOne
    @MapsId("usuarioId")
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "id_rol")
    private Role role;

    public UsuarioHasRoles(Usuario usuario, Role role) {
        this.usuario = usuario;
        this.role = role;
        this.id = new UsuarioRoleId(usuario.getId(), role.getId());
    }

    public UsuarioHasRoles() {
    }


}
