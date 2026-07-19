package com.vida.apirest.model.persona;

import com.vida.apirest.model.auth.Usuario;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "empleado",
        indexes = {
                @Index(name = "ix_empleado_dni", columnList = "dni")
        }
)
public class Empleado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = true, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = true, length = 100)
    private String apellido;

    @Column(name = "image", length = 255, nullable = true)
    private String image;

    @Column(name = "dni", length = 20)
    private String dni;

    @Column(name = "email", length = 100, unique = true, nullable = true)
    private String email;

    @Column(name = "celular", length = 100, unique = true, nullable = true)
    private String celular;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // Relación con Usuario (uno a uno) - sin cascade para evitar conflictos con entidades detached
    @OneToOne
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }



    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
