package com.vida.apirest.model.persona;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "direccion")
public class Direccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "pais", nullable = true, length = 100)
    private String pais;

    @Column(name = "provincia", nullable = true, length = 100)
    private String provincia;

    @Column(name = "localidad", nullable = true, length = 100)
    private String localidad;

    @Column(name = "barrio", nullable = true, length = 100)
    private String barrio;

    @Column(name = "calle", nullable = true, length = 150)
    private String calle;

    @Column(name = "numero", nullable = true, length = 20)
    private String numero;

    @Column(name = "observacion", nullable = true, length = 255)
    private String observacion;
}
