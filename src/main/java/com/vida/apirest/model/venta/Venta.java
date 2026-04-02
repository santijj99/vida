package com.vida.apirest.model.venta;

import com.vida.apirest.model.Cliente;
import com.vida.apirest.model.Empleado;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "venta")
public class Venta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Long idVenta;

    @ManyToOne
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;

    @Column(name = "numero_factura", unique = true, length = 50)
    private String numeroFactura;

    @Column(name = "fecha_venta", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date fechaVenta;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "impuesto", precision = 10, scale = 2)
    private BigDecimal impuesto;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "comision", precision = 10, scale = 2)
    private BigDecimal comision; // Comisión del empleado

    @Column(name = "estado", length = 20)
    private String estado; // COMPLETADA, CANCELADA, PENDIENTE

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago; // EFECTIVO, TARJETA DE CREDITO, TARJETA DE DEBITO, QR, CREDITO, TRANSFERENCIA, CHEQUE

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
    private List<VentaDetalle> detalleVentas;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
    private List<PagoVenta> pagosVenta;

    // Constructores
    public Venta() {
        this.estado = "COMPLETADA";
    }

    public Venta(Cliente cliente, Date fechaVenta, BigDecimal total) {
        this.cliente = cliente;
        this.fechaVenta = fechaVenta;
        this.total = total;
        this.subtotal = total;
        this.impuesto = BigDecimal.ZERO;
        this.estado = "COMPLETADA";
    }

    // Getters y Setters
    public Long getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(Long idVenta) {
        this.idVenta = idVenta;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public Date getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(Date fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getImpuesto() {
        return impuesto;
    }

    public void setImpuesto(BigDecimal impuesto) {
        this.impuesto = impuesto;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getComision() {
        return comision;
    }

    public void setComision(BigDecimal comision) {
        this.comision = comision;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<VentaDetalle> getDetalleVentas() {
        return detalleVentas;
    }

    public void setDetalleVentas(List<VentaDetalle> ventaDetalles) {
        this.detalleVentas = ventaDetalles;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public List<PagoVenta> getPagosVenta() {
        return pagosVenta;
    }

    public void setPagosVenta(List<PagoVenta> pagosVenta) {
        this.pagosVenta = pagosVenta;
    }

    @Override
    public String toString() {
        return "Venta{" + "idVenta=" + idVenta + ", numeroFactura=" + numeroFactura + ", fechaVenta=" + fechaVenta + ", total=" + total + ", estado=" + estado + '}';
    }
}
