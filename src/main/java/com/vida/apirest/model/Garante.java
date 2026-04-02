package com.vida.apirest.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "garante")
public class Garante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = true, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = true, length = 100)
    private String apellido;

    @Column(name = "dni", length = 20)
    private String dni;

    @Column(length = 36, nullable = true)
    private String image;
}
