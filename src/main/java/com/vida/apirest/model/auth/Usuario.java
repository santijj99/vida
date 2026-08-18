package com.vida.apirest.model.auth;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.vida.apirest.model.persona.Empleado;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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

    @Column(name = "reset_codigo", length = 64)
    private String resetCodigo;

    @Column(name = "reset_codigo_expira_at")
    private LocalDateTime resetCodigoExpiraAt;

    @Column(name = "reset_intentos")
    private Integer resetIntentos = 0;

    /** true = password temporal de bootstrap; no puede usar el sistema hasta cambiarla. */
    @Column(name = "debe_cambiar_password")
    private Boolean debeCambiarPassword = false;

    /**
     * Versión de sesión: va en el JWT ({@code ver}). Sube al cambiar password, rol,
     * permisos o al desactivar, así los tokens ya emitidos dejan de valer.
     */
    @Column(name = "token_version", nullable = false)
    @ColumnDefault("0")
    private Integer tokenVersion = 0;

    public int tokenVersionOrZero() {
        return tokenVersion == null ? 0 : tokenVersion;
    }

    public void invalidarTokens() {
        this.tokenVersion = tokenVersionOrZero() + 1;
    }

    public boolean debeCambiarPassword() {
        return Boolean.TRUE.equals(debeCambiarPassword);
    }

    /** Usuario de soporte ATHLAND (visible, con vencimiento). */
    @Column(name = "es_soporte")
    private Boolean esSoporte = false;

    @Column(name = "soporte_expira_at")
    private java.time.Instant soporteExpiraAt;

    @Column(name = "soporte_token_hash", length = 64)
    private String soporteTokenHash;

    public boolean esSoporte() {
        return Boolean.TRUE.equals(esSoporte);
    }

    public boolean soporteVencido() {
        return esSoporte() && soporteExpiraAt != null && !soporteExpiraAt.isAfter(java.time.Instant.now());
    }

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
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<UsuarioHasRoles> usuarioHasRoles = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_principal_id")
    private Role rolPrincipal;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.usuarioHasRoles.stream()
                .map(uhr -> new SimpleGrantedAuthority("ROLE_" + uhr.getRole().getNombre()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEnabled() {
        return activo == null || activo;
    }

    @Override
    public String getUsername() {
        return this.usuario;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

}
