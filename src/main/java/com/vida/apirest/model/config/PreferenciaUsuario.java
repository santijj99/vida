package com.vida.apirest.model.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(
        name = "preferencia_usuario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_preferencia_usuario_clave",
                columnNames = {"usuario_id", "clave"}
        ),
        indexes = @Index(name = "ix_preferencia_clave", columnList = "clave")
)
public class PreferenciaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = true)
    private Long usuarioId;

    @Column(name = "clave", nullable = false, length = 100)
    private String clave;

    @Column(name = "valor", columnDefinition = "TEXT", nullable = false)
    private String valor;
}
