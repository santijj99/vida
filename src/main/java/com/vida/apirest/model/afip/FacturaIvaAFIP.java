package com.vida.apirest.model.afip;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "factura_iva_afip")
public class FacturaIvaAFIP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_iva")
    private Long idIva;

    @ManyToOne
    @JoinColumn(name = "id_factura_afip", nullable = false)
    private FacturaAFIP facturaAFIP;

    @Column(name = "id_iva_tipo", nullable = false)
    private Integer idIvaTipo; // 5=21%, 4=10.5%, 6=27%, etc.

    @Column(name = "base_imp", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseImp;

    @Column(name = "importe", nullable = false, precision = 15, scale = 2)
    private BigDecimal importe;

    // Constructores
    public FacturaIvaAFIP() {
    }

    public FacturaIvaAFIP(FacturaAFIP facturaAFIP, Integer idIvaTipo, BigDecimal baseImp, BigDecimal importe) {
        this.facturaAFIP = facturaAFIP;
        this.idIvaTipo = idIvaTipo;
        this.baseImp = baseImp;
        this.importe = importe;
    }

    // Getters y Setters
    public Long getIdIva() {
        return idIva;
    }

    public void setIdIva(Long idIva) {
        this.idIva = idIva;
    }

    public FacturaAFIP getFacturaAFIP() {
        return facturaAFIP;
    }

    public void setFacturaAFIP(FacturaAFIP facturaAFIP) {
        this.facturaAFIP = facturaAFIP;
    }

    public Integer getIdIvaTipo() {
        return idIvaTipo;
    }

    public void setIdIvaTipo(Integer idIvaTipo) {
        this.idIvaTipo = idIvaTipo;
    }

    public BigDecimal getBaseImp() {
        return baseImp;
    }

    public void setBaseImp(BigDecimal baseImp) {
        this.baseImp = baseImp;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

}
