package com.vida.apirest.model.afip;

import com.vida.apirest.model.venta.Venta;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "factura_afip")
public class FacturaAFIP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura_afip")
    private Long idFacturaAFIP;

    @OneToOne
    @JoinColumn(name = "id_venta", nullable = false, unique = true)
    private Venta venta;

    @ManyToOne
    @JoinColumn(name = "id_cliente_afip", nullable = false)
    private ClienteAFIP clienteAFIP;

    @Column(name = "pto_vta", nullable = false)
    private Integer ptoVta;

    @Column(name = "cbte_tipo", nullable = false)
    private Integer cbteTipo; // 1=Factura A, 6=Factura B, 11=Factura C

    @Column(name = "cbte_nro", nullable = false)
    private Long cbteNro;

    @Column(name = "cbte_fch", nullable = false, length = 8)
    private String cbteFch; // Formato YYYYMMDD

    @Column(name = "cae", length = 14)
    private String cae;

    @Column(name = "cae_fch_vto", length = 8)
    private String caeFchVto; // Formato YYYYMMDD

    @Column(name = "resultado", length = 1)
    private String resultado; // A=Aprobado, R=Rechazado, O=Observado

    @Column(name = "motivos", length = 500)
    private String motivos; // Motivos de rechazo u observación

    @Column(name = "imp_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal impTotal;

    @Column(name = "imp_tot_conc", precision = 15, scale = 2)
    private BigDecimal impTotConc = BigDecimal.ZERO;

    @Column(name = "imp_neto", nullable = false, precision = 15, scale = 2)
    private BigDecimal impNeto;

    @Column(name = "imp_op_ex", precision = 15, scale = 2)
    private BigDecimal impOpEx = BigDecimal.ZERO;

    @Column(name = "imp_trib", precision = 15, scale = 2)
    private BigDecimal impTrib = BigDecimal.ZERO;

    @Column(name = "imp_iva", precision = 15, scale = 2)
    private BigDecimal impIVA = BigDecimal.ZERO;

    @Column(name = "mon_id", length = 3)
    private String monId = "PES";

    @Column(name = "mon_cotiz", precision = 10, scale = 6)
    private BigDecimal monCotiz = BigDecimal.ONE;

    @Column(name = "concepto", nullable = false)
    private Integer concepto; // 1=Productos, 2=Servicios, 3=Productos y Servicios

    @Column(name = "fecha_emision", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaEmision;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @OneToMany(mappedBy = "facturaAFIP", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaItemAFIP> items;

    @OneToMany(mappedBy = "facturaAFIP", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaIvaAFIP> ivas;

    @OneToMany(mappedBy = "facturaAFIP", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaTributoAFIP> tributos;

    // Constructores
    public FacturaAFIP() {
        this.fechaEmision = new Date();
        this.resultado = "P"; // Pendiente
    }

    // Getters y Setters
    public Long getIdFacturaAFIP() {
        return idFacturaAFIP;
    }

    public void setIdFacturaAFIP(Long idFacturaAFIP) {
        this.idFacturaAFIP = idFacturaAFIP;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public ClienteAFIP getClienteAFIP() {
        return clienteAFIP;
    }

    public void setClienteAFIP(ClienteAFIP clienteAFIP) {
        this.clienteAFIP = clienteAFIP;
    }

    public Integer getPtoVta() {
        return ptoVta;
    }

    public void setPtoVta(Integer ptoVta) {
        this.ptoVta = ptoVta;
    }

    public Integer getCbteTipo() {
        return cbteTipo;
    }

    public void setCbteTipo(Integer cbteTipo) {
        this.cbteTipo = cbteTipo;
    }

    public Long getCbteNro() {
        return cbteNro;
    }

    public void setCbteNro(Long cbteNro) {
        this.cbteNro = cbteNro;
    }

    public String getCbteFch() {
        return cbteFch;
    }

    public void setCbteFch(String cbteFch) {
        this.cbteFch = cbteFch;
    }

    public String getCae() {
        return cae;
    }

    public void setCae(String cae) {
        this.cae = cae;
    }

    public String getCaeFchVto() {
        return caeFchVto;
    }

    public void setCaeFchVto(String caeFchVto) {
        this.caeFchVto = caeFchVto;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getMotivos() {
        return motivos;
    }

    public void setMotivos(String motivos) {
        this.motivos = motivos;
    }

    public BigDecimal getImpTotal() {
        return impTotal;
    }

    public void setImpTotal(BigDecimal impTotal) {
        this.impTotal = impTotal;
    }

    public BigDecimal getImpTotConc() {
        return impTotConc;
    }

    public void setImpTotConc(BigDecimal impTotConc) {
        this.impTotConc = impTotConc;
    }

    public BigDecimal getImpNeto() {
        return impNeto;
    }

    public void setImpNeto(BigDecimal impNeto) {
        this.impNeto = impNeto;
    }

    public BigDecimal getImpOpEx() {
        return impOpEx;
    }

    public void setImpOpEx(BigDecimal impOpEx) {
        this.impOpEx = impOpEx;
    }

    public BigDecimal getImpTrib() {
        return impTrib;
    }

    public void setImpTrib(BigDecimal impTrib) {
        this.impTrib = impTrib;
    }

    public BigDecimal getImpIVA() {
        return impIVA;
    }

    public void setImpIVA(BigDecimal impIVA) {
        this.impIVA = impIVA;
    }

    public String getMonId() {
        return monId;
    }

    public void setMonId(String monId) {
        this.monId = monId;
    }

    public BigDecimal getMonCotiz() {
        return monCotiz;
    }

    public void setMonCotiz(BigDecimal monCotiz) {
        this.monCotiz = monCotiz;
    }

    public Integer getConcepto() {
        return concepto;
    }

    public void setConcepto(Integer concepto) {
        this.concepto = concepto;
    }

    public Date getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(Date fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<FacturaItemAFIP> getItems() {
        return items;
    }

    public void setItems(List<FacturaItemAFIP> items) {
        this.items = items;
    }

    public List<FacturaIvaAFIP> getIvas() {
        return ivas;
    }

    public void setIvas(List<FacturaIvaAFIP> ivas) {
        this.ivas = ivas;
    }

    public List<FacturaTributoAFIP> getTributos() {
        return tributos;
    }

    public void setTributos(List<FacturaTributoAFIP> tributos) {
        this.tributos = tributos;
    }
}
