package com.vida.apirest.model;


import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "usuario")
public class Usuario implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(length = 36, unique = true, nullable = true)
    private String image;

    @Column(name = "usuario", nullable = false, unique = true, length = 100)
    private String usuario;

    @Column(name = "email", length = 100, unique = true, nullable = true)
    private String email;

    @Column(name = "celular", length = 100, unique = true, nullable = true)
    private String celular;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "activo", nullable = true)
    private Boolean activo = true;

    @Column(name = "notificacion_token", length = 100, nullable = true)
    private String notificacionToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt = LocalDate.now();

    // Relación con Empleado (uno a uno)
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL)
    private Empleado empleado;

//    // Relación con Roles (muchos a muchos)
//    @ManyToMany
//    @JoinTable(
//            name = "usuario_rol",
//            joinColumns = @JoinColumn(name = "usuario_id"),
//            inverseJoinColumns = @JoinColumn(name = "rol_id")
//    )
//    private List<Role> roles;

    //tabla con la cual me relaciono "usuario"
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UsuarioHasRoles> usuarioHasRoles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return this.email;
    }
}
