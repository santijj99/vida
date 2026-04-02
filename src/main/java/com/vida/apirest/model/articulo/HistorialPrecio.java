package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "historial_precio",
    indexes = {
        @Index(name = "ix_hist_precio_variante", columnList = "variante_articulo_id"),
        @Index(name = "ix_hist_precio_fecha", columnList = "fecha")
    }
)
public class HistorialPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "variante_articulo_id", nullable = false)
    private Long varianteArticuloId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variante_articulo_id", insertable = false, updatable = false)
    private VarianteArticulo varianteArticulo;

    @Column(name = "precio_anterior", precision = 12, scale = 2)
    private BigDecimal precioAnterior;

    @Column(name = "precio_nuevo", precision = 12, scale = 2, nullable = false)
    private BigDecimal precioNuevo;

    @Column(name = "costo_anterior", precision = 12, scale = 2)
    private BigDecimal costoAnterior;

    @Column(name = "costo_nuevo", precision = 12, scale = 2)
    private BigDecimal costoNuevo;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVarianteArticuloId() {
        return varianteArticuloId;
    }

    public void setVarianteArticuloId(Long varianteArticuloId) {
        this.varianteArticuloId = varianteArticuloId;
    }

    public VarianteArticulo getVarianteArticulo() {
        return varianteArticulo;
    }

    public BigDecimal getPrecioAnterior() {
        return precioAnterior;
    }

    public void setPrecioAnterior(BigDecimal precioAnterior) {
        this.precioAnterior = precioAnterior;
    }

    public BigDecimal getPrecioNuevo() {
        return precioNuevo;
    }

    public void setPrecioNuevo(BigDecimal precioNuevo) {
        this.precioNuevo = precioNuevo;
    }

    public BigDecimal getCostoAnterior() {
        return costoAnterior;
    }

    public void setCostoAnterior(BigDecimal costoAnterior) {
        this.costoAnterior = costoAnterior;
    }

    public BigDecimal getCostoNuevo() {
        return costoNuevo;
    }

    public void setCostoNuevo(BigDecimal costoNuevo) {
        this.costoNuevo = costoNuevo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
