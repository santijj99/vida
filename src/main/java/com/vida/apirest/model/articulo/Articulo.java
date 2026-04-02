package com.vida.apirest.model.articulo;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "articulo",
        indexes = {
                @Index(name = "ix_articulo_marca", columnList = "marca_id"),
                @Index(name = "ix_articulo_categoria", columnList = "categoria_id"),
                @Index(name = "ix_articulo_genero", columnList = "genero_id"),
                @Index(name = "ix_articulo_codigo", columnList = "codigo", unique = true)
        }
)
public class Articulo {

    public enum EstadoProducto { ACTIVO, BORRADOR, ARCHIVADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "marca_id")
    private Long marcaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marca_id", insertable = false, updatable = false)
    private Marca marca;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", insertable = false, updatable = false)
    private Categoria categoria;

    @Column(name = "genero_id")
    private Long generoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genero_id", insertable = false, updatable = false)
    private Genero genero;

    @Column(length = 255, nullable = false)
    private String codigo;

    @Column(length = 255, nullable = false)
    private String modelo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoProducto estado = EstadoProducto.ACTIVO;

    // Relaciones bidireccionales
    @OneToMany(mappedBy = "articulo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VarianteArticulo> variantes;

    @OneToMany(mappedBy = "articulo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImagenArticulo> imagenes;

    @OneToMany(mappedBy = "articulo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaxonArticulo> taxones;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // getters/setters
    public Long getId() {
        return id;
    }

    public Long getMarcaId() {
        return marcaId;
    }

    public void setMarcaId(Long marcaId) {
        this.marcaId = marcaId;
    }

    public Marca getMarca() {
        return marca;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Long getGeneroId() {
        return generoId;
    }

    public void setGeneroId(Long generoId) {
        this.generoId = generoId;
    }

    public Genero getGenero() {
        return genero;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoProducto getEstado() {
        return estado;
    }

    public void setEstado(EstadoProducto estado) {
        this.estado = estado;
    }

    public List<VarianteArticulo> getVariantes() {
        return variantes;
    }

    public void setVariantes(List<VarianteArticulo> variantes) {
        this.variantes = variantes;
    }

    public List<ImagenArticulo> getImagenes() {
        return imagenes;
    }

    public void setImagenes(List<ImagenArticulo> imagenes) {
        this.imagenes = imagenes;
    }

    public List<TaxonArticulo> getTaxones() {
        return taxones;
    }

    public void setTaxones(List<TaxonArticulo> taxones) {
        this.taxones = taxones;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}