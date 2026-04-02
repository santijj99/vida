package com.vida.apirest.model.afip;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "factura_item_afip")
public class FacturaItemAFIP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private Long idItem;

    @ManyToOne
    @JoinColumn(name = "id_factura_afip", nullable = false)
    private FacturaAFIP facturaAFIP;

    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    @Column(name = "cantidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "codigo", length = 50)
    private String codigo;

    // Constructores
    public FacturaItemAFIP() {
    }

    public FacturaItemAFIP(FacturaAFIP facturaAFIP, String descripcion, BigDecimal cantidad, BigDecimal precioUnitario) {
        this.facturaAFIP = facturaAFIP;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = precioUnitario.multiply(cantidad);
    }

    // Getters y Setters
    public Long getIdItem() {
        return idItem;
    }

    public void setIdItem(Long idItem) {
        this.idItem = idItem;
    }

    public FacturaAFIP getFacturaAFIP() {
        return facturaAFIP;
    }

    public void setFacturaAFIP(FacturaAFIP facturaAFIP) {
        this.facturaAFIP = facturaAFIP;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
        if (precioUnitario != null) {
            this.subtotal = precioUnitario.multiply(cantidad);
        }
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
        if (cantidad != null) {
            this.subtotal = precioUnitario.multiply(cantidad);
        }
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
