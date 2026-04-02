package com.vida.apirest.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(exclude = "cliente")
@ToString(exclude = "cliente")
@Entity
@Table(name = "contacto")
public class Contacto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "nombre", nullable = true, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = true, length = 100)
    private String apellido;

    @Column(name = "email", nullable = true, length = 100)
    private String email;

    @Column(name = "telefono", nullable = true, length = 20)
    private String telefono;
}
