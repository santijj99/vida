package com.vida.apirest.model.auth.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
@Embeddable
public class UsuarioRoleId implements Serializable {

    @Column(name = "id_usuario")
    private Long usuarioId;

    @Column(name = "id_rol")
    private Long roleId;

   @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof UsuarioRoleId)) return false;
        UsuarioRoleId usuarioRoleId = (UsuarioRoleId) o;
        return Objects.equals(usuarioId, usuarioRoleId.usuarioId) && Objects.equals(roleId, usuarioRoleId.roleId);
    }

    @Override
    public int hashCode(){
        return Objects.hash(usuarioId, roleId);
    }

    public UsuarioRoleId(){}

    public UsuarioRoleId(Long userId, Long roleId){
        this.usuarioId = userId;
        this.roleId = roleId;
    }
}
