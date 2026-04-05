package com.vida.apirest.model.persona;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(exclude = {"garante", "garantizados", "contactos"})
@ToString(exclude = {"garante", "garantizados", "contactos"})
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

    @ManyToOne
    @JoinColumn(name = "garante_id")
    private Cliente garante;

    @OneToMany(mappedBy = "garante", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Cliente> garantizados = new HashSet<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Contacto> contactos = new HashSet<>();

    @Column(name = "dni", length = 20)
    private String dni;

    @Column(length = 36, nullable = true)
    private String image;
}
