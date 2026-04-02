package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(
        name = "variante_articulo",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_variante_articulo_color_talle",
                        columnNames = {"articulo_id", "color_id", "talle_id"}
                ),
                @UniqueConstraint(
                        name = "uk_variante_codigo_barras",
                        columnNames = {"codigo_barras"}
                )
        },
        indexes = {
                @Index(name = "ix_var_articulo", columnList = "articulo_id"),
                @Index(name = "ix_var_color", columnList = "color_id"),
                @Index(name = "ix_var_talle", columnList = "talle_id"),
                @Index(name = "ix_var_lista_precio", columnList = "lista_precio_id")
        }
)
public class VarianteArticulo {

    public Long getListaPrecioId() {
        return listaPrecioId;
    }

    public void setListaPrecioId(Long listaPrecioId) {
        this.listaPrecioId = listaPrecioId;
    }

    public enum EstadoVariante { ACTIVO, INACTIVO, PENDIENTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "articulo_id", nullable = false)
    private Long articuloId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "articulo_id", insertable = false, updatable = false)
    private Articulo articulo;

    @Column(name = "color_id")
    private Long colorId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "color_id", insertable = false, updatable = false)
    private Color color;


    @Column(name = "lista_precio_id")
    private Long listaPrecioId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lista_precio_id", insertable = false, updatable = false)
    private ListaPrecio listaPrecio;



    @Column(name = "talle_id")
    private Long talleId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "talle_id", insertable = false, updatable = false)
    private Talle talle;

    @Column(name = "codigo_barras", length = 64, unique = true)
    private String codigoBarras;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVariante estado = EstadoVariante.ACTIVO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relación bidireccional con HistorialPrecio
    @OneToMany(mappedBy = "varianteArticulo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistorialPrecio> historialPrecios;

    // getters/setters
    public Long getId() { return id; }

    public Long getArticuloId() { return articuloId; }
    public void setArticuloId(Long articuloId) { this.articuloId = articuloId; }
    public Articulo getArticulo() { return articulo; }

    public Long getColorId() { return colorId; }
    public void setColorId(Long colorId) { this.colorId = colorId; }
    public Color getColor() { return color; }

    public Long getTalleId() { return talleId; }
    public void setTalleId(Long talleId) { this.talleId = talleId; }
    public Talle getTalle() { return talle; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public EstadoVariante getEstado() { return estado; }
    public void setEstado(EstadoVariante estado) { this.estado = estado; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public ListaPrecio getListaPrecio() {
        return listaPrecio;
    }

    public void setListaPrecio(ListaPrecio listaPrecio) {
        this.listaPrecio = listaPrecio;
    }

    public List<HistorialPrecio> getHistorialPrecios() {
        return historialPrecios;
    }

    public void setHistorialPrecios(List<HistorialPrecio> historialPrecios) {
        this.historialPrecios = historialPrecios;
    }
}