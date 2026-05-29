package com.vida.apirest.model.persona;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(exclude = {"garante", "garantizados", "contactos", "direccion"})
@ToString(exclude = {"garante", "garantizados", "contactos", "direccion"})
@Entity
@Table(name = "cliente")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = true, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = true, length = 100)
    private String apellido;

    @Column(name = "email", nullable = true, length = 100)
    private String email;

    @Column(name = "telefono", nullable = true, length = 30)
    private String telefono;

    @Column(name = "tabajo", nullable = true, length = 100)
    private String trabajo;

    @ManyToOne
    @JoinColumn(name = "garante_id")
    private Cliente garante;

    @OneToMany(mappedBy = "garante", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Cliente> garantizados = new HashSet<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Contacto> contactos = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "direccion_id")
    private Direccion direccion;

    @Column(name = "dni", length = 20)
    private String dni;
}
