package com.vida.apirest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @Column(length = 36)
    private  String id = UUID.randomUUID().toString();

    @Column(length = 36, unique = true, nullable = false)
    private String nombre;

    @Column(length = 36, unique = true, nullable = true)
    private String image;

    @Column(length = 36, unique = true, nullable = true)
    private String route;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt = LocalDate.now();

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UsuarioHasRoles> usuariosHasRoles = new HashSet<>();

}
