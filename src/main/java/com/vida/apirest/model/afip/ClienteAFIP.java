package com.vida.apirest.model.afip;

import com.vida.apirest.model.persona.Cliente;
import jakarta.persistence.*;

@Entity
@Table(name = "cliente_afip")
public class ClienteAFIP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente_afip")
    private Long idClienteAFIP;

    @OneToOne
    @JoinColumn(name = "id_cliente", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "doc_tipo", nullable = false)
    private Integer docTipo; // 80=CUIT, 96=DNI, 99=Sin identificar

    @Column(name = "doc_nro", nullable = false, length = 20)
    private String docNro;

    @Column(name = "condicion_iva_receptor_id", nullable = false)
    private Integer condicionIVAReceptorId; // 1=IVA Responsable Inscripto, 4=IVA Sujeto Exento, etc.

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(name = "domicilio", length = 300)
    private String domicilio;

    // Constructores
    public ClienteAFIP() {
    }

    public ClienteAFIP(Cliente cliente, Integer docTipo, String docNro, Integer condicionIVAReceptorId) {
        this.cliente = cliente;
        this.docTipo = docTipo;
        this.docNro = docNro;
        this.condicionIVAReceptorId = condicionIVAReceptorId;
    }

    // Getters y Setters
    public Long getIdClienteAFIP() {
        return idClienteAFIP;
    }

    public void setIdClienteAFIP(Long idClienteAFIP) {
        this.idClienteAFIP = idClienteAFIP;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Integer getDocTipo() {
        return docTipo;
    }

    public void setDocTipo(Integer docTipo) {
        this.docTipo = docTipo;
    }

    public String getDocNro() {
        return docNro;
    }

    public void setDocNro(String docNro) {
        this.docNro = docNro;
    }

    public Integer getCondicionIVAReceptorId() {
        return condicionIVAReceptorId;
    }

    public void setCondicionIVAReceptorId(Integer condicionIVAReceptorId) {
        this.condicionIVAReceptorId = condicionIVAReceptorId;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio;
    }
}
