package com.vida.apirest.model.auth;

import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.auth.id.UsuarioSucursalId;
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

@Entity
@Data
@NoArgsConstructor
@Table(name = "usuario_sucursal")
public class UsuarioSucursal {

    @EmbeddedId
    private UsuarioSucursalId id = new UsuarioSucursalId();

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(optional = false)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(optional = false)
    @MapsId("sucursalId")
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    public UsuarioSucursal(Usuario usuario, Sucursal sucursal) {
        this.usuario = usuario;
        this.sucursal = sucursal;
        if (usuario != null && usuario.getId() != null && sucursal != null && sucursal.getId() != null) {
            this.id = new UsuarioSucursalId(usuario.getId(), sucursal.getId());
        }
    }
}
