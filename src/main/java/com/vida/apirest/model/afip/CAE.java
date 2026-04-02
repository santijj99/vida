package com.vida.apirest.model.afip;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;
@Entity
@Table(name = "cae")
public class CAE {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cae")
    private Long idCAE;

    @Column(name = "pto_vta", nullable = false)
    private Integer ptoVta;

    @Column(name = "cbte_tipo", nullable = false)
    private Integer cbteTipo;

    @Column(name = "ultimo_cbte_nro", nullable = false)
    private Long ultimoCbteNro;

    @Column(name = "fecha_actualizacion", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaActualizacion;

    // Constructores
    public CAE() {
        this.fechaActualizacion = new Date();
    }

    public CAE(Integer ptoVta, Integer cbteTipo, Long ultimoCbteNro) {
        this.ptoVta = ptoVta;
        this.cbteTipo = cbteTipo;
        this.ultimoCbteNro = ultimoCbteNro;
        this.fechaActualizacion = new Date();
    }

    // Getters y Setters
    public Long getIdCAE() {
        return idCAE;
    }

    public void setIdCAE(Long idCAE) {
        this.idCAE = idCAE;
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

    public Long getUltimoCbteNro() {
        return ultimoCbteNro;
    }

    public void setUltimoCbteNro(Long ultimoCbteNro) {
        this.ultimoCbteNro = ultimoCbteNro;
    }

    public Date getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(Date fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
