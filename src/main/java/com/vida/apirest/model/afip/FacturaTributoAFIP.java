package com.vida.apirest.model.afip;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "factura_tributo_afip")
public class FacturaTributoAFIP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tributo")
    private Long idTributo;

    @ManyToOne
    @JoinColumn(name = "id_factura_afip", nullable = false)
    private FacturaAFIP facturaAFIP;

    @Column(name = "id_tributo_tipo", nullable = false)
    private Integer idTributoTipo; // 99=Impuestos Municipales, etc.

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "base_imp", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseImp;

    @Column(name = "alic", precision = 10, scale = 2)
    private BigDecimal alic; // Alícuota

    @Column(name = "importe", nullable = false, precision = 15, scale = 2)
    private BigDecimal importe;

    // Constructores
    public FacturaTributoAFIP() {
    }

    public FacturaTributoAFIP(FacturaAFIP facturaAFIP, Integer idTributoTipo, String descripcion,
                              BigDecimal baseImp, BigDecimal alic, BigDecimal importe) {
        this.facturaAFIP = facturaAFIP;
        this.idTributoTipo = idTributoTipo;
        this.descripcion = descripcion;
        this.baseImp = baseImp;
        this.alic = alic;
        this.importe = importe;
    }

    // Getters y Setters
    public Long getIdTributo() {
        return idTributo;
    }

    public void setIdTributo(Long idTributo) {
        this.idTributo = idTributo;
    }

    public FacturaAFIP getFacturaAFIP() {
        return facturaAFIP;
    }

    public void setFacturaAFIP(FacturaAFIP facturaAFIP) {
        this.facturaAFIP = facturaAFIP;
    }

    public Integer getIdTributoTipo() {
        return idTributoTipo;
    }

    public void setIdTributoTipo(Integer idTributoTipo) {
        this.idTributoTipo = idTributoTipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getBaseImp() {
        return baseImp;
    }

    public void setBaseImp(BigDecimal baseImp) {
        this.baseImp = baseImp;
    }

    public BigDecimal getAlic() {
        return alic;
    }

    public void setAlic(BigDecimal alic) {
        this.alic = alic;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }
}
