package com.vida.apirest.servicies;

import com.vida.apirest.dto.venta.CajaCuentaResponse;
import com.vida.apirest.dto.venta.CajaMovimientoResponse;
import com.vida.apirest.dto.venta.CreditoSimulacionRequest;
import com.vida.apirest.dto.venta.CreditoSimulacionResponse;
import com.vida.apirest.dto.venta.PagoVentaRequest;
import com.vida.apirest.dto.venta.PagoVentaResponse;
import com.vida.apirest.dto.venta.VentaCreateRequest;
import com.vida.apirest.dto.venta.VentaCreditoPersonalRequest;
import com.vida.apirest.dto.venta.VentaDetalleResponse;
import com.vida.apirest.dto.venta.VentaResponse;
import com.vida.apirest.model.almacen.Stock;
import com.vida.apirest.model.almacen.StockMovimiento;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.model.articulo.VarianteArticulo;
import com.vida.apirest.model.credito.Credito;
import com.vida.apirest.model.credito.Cuota;
import com.vida.apirest.model.credito.Cuenta;
import com.vida.apirest.model.finanzas.CuentaFinanciera;
import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.model.persona.Empleado;
import com.vida.apirest.model.tesoreria.MovimientoFinanciero;
import com.vida.apirest.model.venta.PagoVenta;
import com.vida.apirest.model.venta.Venta;
import com.vida.apirest.model.venta.VentaDetalle;
import com.vida.apirest.repositories.ArticuloRepository;
import com.vida.apirest.repositories.ClienteRepository;
import com.vida.apirest.repositories.CreditoRepository;
import com.vida.apirest.repositories.CuentaRepository;
import com.vida.apirest.repositories.EmpleadoRepository;
import com.vida.apirest.repositories.FinanzasCuentaFinancieraRepository;
import com.vida.apirest.repositories.MovimientoFinancieroRepository;
import com.vida.apirest.repositories.PagoVentaRepository;
import com.vida.apirest.repositories.SucursalRepository;
import com.vida.apirest.repositories.StockMovimientoRepository;
import com.vida.apirest.repositories.StockRepository;
import com.vida.apirest.repositories.VentaRepository;
import com.vida.apirest.repositories.VarianteArticuloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ArticuloRepository articuloRepository;
    private final VarianteArticuloRepository varianteArticuloRepository;
    private final StockRepository stockRepository;
    private final StockMovimientoRepository stockMovimientoRepository;
    private final SucursalRepository sucursalRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final FinanzasCuentaFinancieraRepository cuentaRepository;
    private final CuentaRepository creditoCuentaRepository;
    private final CreditoRepository creditoRepository;
    private final MovimientoFinancieroRepository movimientoFinancieroRepository;

    @Transactional
    public VentaResponse registrarVenta(VentaCreateRequest request) {
        if (request.getClienteDni() == null || request.getClienteDni().isBlank()) {
            throw new RuntimeException("DNI de cliente requerido para registrar la venta");
        }

        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos un detalle de venta");
        }

        Cliente cliente = clienteRepository.findByDni(request.getClienteDni())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con DNI: " + request.getClienteDni()));

        if (request.getSucursalId() == null) {
            throw new RuntimeException("Sucursal requerida para registrar la venta");
        }

        var sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con ID: " + request.getSucursalId()));

        Empleado empleado = null;
        if (request.getEmpleadoId() != null) {
            empleado = empleadoRepository.findById(request.getEmpleadoId())
                    .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + request.getEmpleadoId()));
        }

        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setSucursal(sucursal);
        venta.setEmpleado(empleado);
        venta.setNumeroFactura(request.getNumeroFactura() != null && !request.getNumeroFactura().isBlank()
                ? request.getNumeroFactura()
                : "FV-" + UUID.randomUUID().toString().replace("-", ""));
        venta.setFechaVenta(request.getFechaVenta() != null ? request.getFechaVenta() : LocalDateTime.now());
        venta.setObservaciones(request.getObservaciones());
        venta.setMetodoPago(request.getMetodoPago());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        BigDecimal impuesto = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (var detalleReq : request.getDetalles()) {
            VarianteArticulo variante = null;
            Articulo articulo = null;

            if (detalleReq.getVarianteId() != null) {
                Long varianteId = detalleReq.getVarianteId();
                variante = varianteArticuloRepository.findById(varianteId)
                        .orElseThrow(() -> new RuntimeException("Variante no encontrada con ID: " + varianteId));
                Long articuloId = variante.getArticuloId();
                articulo = articuloRepository.findById(articuloId)
                        .orElseThrow(() -> new RuntimeException("Artículo de la variante no encontrado con ID: " + articuloId));
            } else {
                if (detalleReq.getArticuloId() == null) {
                    throw new RuntimeException("Cada detalle requiere articuloId o varianteId");
                }
                Long articuloId = detalleReq.getArticuloId();
                articulo = articuloRepository.findById(articuloId)
                        .orElseThrow(() -> new RuntimeException("Artículo no encontrado con ID: " + articuloId));
            }

            if (detalleReq.getCantidad() == null || detalleReq.getCantidad() <= 0) {
                throw new RuntimeException("La cantidad del detalle debe ser mayor a cero");
            }

            BigDecimal precioUnitario;
            if (variante != null) {
                precioUnitario = obtenerPrecioUnitarioDesdeVariante(variante);
                if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("No existe precio unitario válido para la variante con ID: " + variante.getId());
                }
            } else {
                if (detalleReq.getPrecioUnitario() == null || detalleReq.getPrecioUnitario().compareTo(BigDecimal.ZERO) < 0) {
                    throw new RuntimeException("El precio unitario del detalle debe ser un valor válido");
                }
                precioUnitario = detalleReq.getPrecioUnitario();
            }

            Stock stock = variante != null
                    ? findStockByVariante(variante.getId(), sucursal.getId())
                    : findStock(articulo.getId(), null, sucursal.getId());
            ajustarStock(stock, detalleReq.getCantidad(), venta.getNumeroFactura());

            VentaDetalle detalle = new VentaDetalle();
            detalle.setVenta(venta);
            detalle.setArticulo(articulo);
            detalle.setVariante(variante);
            detalle.setCantidad(detalleReq.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setDescuentoPorcentaje(detalleReq.getDescuentoPorcentaje() != null ? detalleReq.getDescuentoPorcentaje() : BigDecimal.ZERO);
            detalle.setDescuentoMonto(detalleReq.getDescuentoMonto() != null ? detalleReq.getDescuentoMonto() : BigDecimal.ZERO);
            detalle.setImpuesto(detalleReq.getImpuesto() != null ? detalleReq.getImpuesto() : BigDecimal.ZERO);
            detalle.setLote(detalleReq.getLote());
            detalle.setNumeroSerie(detalleReq.getNumeroSerie());

            BigDecimal detalleSubtotal = detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()))
                    .subtract(detalle.getDescuentoMonto());
            BigDecimal detalleTotal = detalleSubtotal.add(detalle.getImpuesto());

            detalle.setSubtotal(detalleSubtotal);
            detalle.setTotal(detalleTotal);

            subtotal = subtotal.add(detalleSubtotal);
            descuento = descuento.add(detalle.getDescuentoMonto());
            impuesto = impuesto.add(detalle.getImpuesto());
            total = total.add(detalleTotal);

            venta.getDetalles().add(detalle);
        }

        venta.setSubtotal(subtotal);
        venta.setDescuento(descuento);
        venta.setImpuesto(impuesto);
        venta.setTotal(total);
        venta.setEstado((request.getPagos() != null && !request.getPagos().isEmpty()) ? Venta.EstadoVenta.CONFIRMADA : Venta.EstadoVenta.BORRADOR);

        Venta ventaGuardada = ventaRepository.save(venta);

        if (request.getPagos() != null && !request.getPagos().isEmpty()) {
            for (PagoVentaRequest pagoReq : request.getPagos()) {
                if (pagoReq.getMonto() == null || pagoReq.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("Cada pago debe tener un monto mayor a cero");
                }

                PagoVenta pago = new PagoVenta();
                pago.setVenta(ventaGuardada);
                pago.setMonto(pagoReq.getMonto());
                pago.setMetodoPago(pagoReq.getMetodoPago());
                pago.setReferencia(pagoReq.getReferencia());
                pago.setNumeroComprobante(pagoReq.getNumeroComprobante());
                pago.setObservaciones(pagoReq.getObservaciones());
                pago.setNumero("PV-" + UUID.randomUUID().toString().replace("-", ""));

                if (pagoReq.getMetodoPago() != null && pagoReq.getMetodoPago().equalsIgnoreCase("CREDITO")) {
                    if (pagoReq.getCreditoPlazoMeses() == null || pagoReq.getCreditoPlazoMeses() <= 0) {
                        throw new RuntimeException("Para pagos con crédito se requiere un plazo en meses mayor a cero");
                    }

                    Cuenta cuentaCredito = crearOEncontrarCuentaCredito(cliente, sucursal);
                    Credito credito = new Credito();
                    credito.setCliente(cliente);
                    credito.setSucursal(sucursal);
                    credito.setVenta(ventaGuardada);
                    credito.setNumero("CR-" + cliente.getId() + "-" + sucursal.getId() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
                    credito.setPlazoMeses(pagoReq.getCreditoPlazoMeses());
                    credito.setTasaInteres(pagoReq.getCreditoTasaInteres() != null ? pagoReq.getCreditoTasaInteres() : BigDecimal.ZERO);
                    credito.setDescripcion(pagoReq.getCreditoDescripcion());
                    credito.setEstado(Credito.EstadoCredito.ACTIVO);
                    CreditoPlanificador.ResultadoPlan plan = CreditoPlanificador.planificar(
                            ventaGuardada.getTotal(),
                            pagoReq.getCreditoPlazoMeses(),
                            pagoReq.getCreditoTasaInteres(),
                            pagoReq.getCreditoMontoAnticipo() != null ? pagoReq.getCreditoMontoAnticipo() : BigDecimal.ZERO,
                            pagoReq.getCreditoModoDistribucion(),
                            ventaGuardada.getFechaVenta()
                    );
                    credito.setImporte(plan.montoFinanciado);
                    credito.setSaldo(plan.montoFinanciado);
                    credito.setCuotas(CreditoPlanificador.materializarCuotas(credito, plan));
                    pago.setMonto(plan.montoFinanciado);
                    creditoRepository.save(credito);

                    BigDecimal saldoAnteriorCredito = cuentaCredito.getSaldoActual() != null ? cuentaCredito.getSaldoActual() : BigDecimal.ZERO;
                    cuentaCredito.setSaldoActual(saldoAnteriorCredito.add(plan.montoFinanciado));
                    creditoCuentaRepository.save(cuentaCredito);

                    pago.setEstado(PagoVenta.EstadoPago.PENDIENTE);
                    pago.setObservaciones((pago.getObservaciones() != null ? pago.getObservaciones() + " " : "") + "Crédito generado: " + credito.getNumero());
                    pagoVentaRepository.save(pago);
                    ventaGuardada.getPagos().add(pago);
                } else {
                    pago.setEstado(PagoVenta.EstadoPago.RECIBIDO);
                    CuentaFinanciera cuenta = null;
                    if (pagoReq.getCuentaId() != null) {
                        cuenta = cuentaRepository.findById(pagoReq.getCuentaId())
                                .orElseThrow(() -> new RuntimeException("Cuenta financiera no encontrada con ID: " + pagoReq.getCuentaId()));
                    } else {
                        cuenta = cuentaRepository.findFirstByTipoAndActivoTrue(CuentaFinanciera.TipoCuenta.CAJA)
                                .orElse(null); // No throw exception, allow payment without cash account
                    }

                    if (cuenta != null) {
                        pago.setCuenta(cuenta);
                        pagoVentaRepository.save(pago);
                        ventaGuardada.getPagos().add(pago);
                        registrarMovimientoCaja(cuenta, pago, ventaGuardada.getNumeroFactura());
                    } else {
                        // Save payment without account, mark as pending
                        pago.setEstado(PagoVenta.EstadoPago.PENDIENTE);
                        pagoVentaRepository.save(pago);
                        ventaGuardada.getPagos().add(pago);
                    }
                }
            }
        }

        return mapVentaResponse(ventaRepository.findById(ventaGuardada.getId())
                .orElseThrow(() -> new RuntimeException("Error al recuperar la venta registrada")));
    }

    @Transactional(readOnly = true)
    public CreditoSimulacionResponse simularCreditoPersonal(CreditoSimulacionRequest request) {
        BigDecimal subtotal = resolverSubtotalSimulacion(request);
        int plazo = request.getPlazoMeses() != null ? request.getPlazoMeses() : 1;
        BigDecimal tasa = request.getTasaInteres() != null ? request.getTasaInteres() : BigDecimal.ZERO;
        BigDecimal anticipo = request.getMontoAnticipo() != null ? request.getMontoAnticipo() : BigDecimal.ZERO;
        CreditoPlanificador.ResultadoPlan plan = CreditoPlanificador.planificar(
                subtotal,
                plazo,
                tasa,
                anticipo,
                request.getModoDistribucion(),
                resolverBaseFechaCredito(request.getFechaPrimerVencimiento())
        );
        return CreditoPlanificador.toSimulacionResponse(plan, plazo, tasa);
    }

    @Transactional
    public VentaResponse registrarVentaCreditoPersonal(VentaCreditoPersonalRequest request) {
        if (request.getCreditoPlazoMeses() == null || request.getCreditoPlazoMeses() <= 0) {
            throw new RuntimeException("Se requiere un plazo de crédito personal mayor a cero");
        }

        BigDecimal subtotal = calcularTotalCreditoPersonal(request);
        BigDecimal anticipo = request.getMontoAnticipo() != null ? request.getMontoAnticipo() : BigDecimal.ZERO;
        BigDecimal tasa = request.getCreditoTasaInteres() != null ? request.getCreditoTasaInteres() : BigDecimal.ZERO;

        CreditoPlanificador.ResultadoPlan plan = CreditoPlanificador.planificar(
                subtotal,
                request.getCreditoPlazoMeses(),
                tasa,
                anticipo,
                request.getModoDistribucion(),
                resolverBaseFechaCredito(request.getFechaPrimerVencimiento())
        );

        VentaCreateRequest internalRequest = new VentaCreateRequest();
        internalRequest.setSucursalId(request.getSucursalId());
        internalRequest.setEmpleadoId(request.getEmpleadoId());
        internalRequest.setClienteDni(request.getClienteDni());
        internalRequest.setNumeroFactura(request.getNumeroFactura());
        internalRequest.setFechaVenta(request.getFechaVenta());
        internalRequest.setObservaciones(request.getObservaciones());
        internalRequest.setMetodoPago("CREDITO");
        internalRequest.setDetalles(request.getDetalles());

        List<PagoVentaRequest> pagos = new ArrayList<>();
        if (anticipo.compareTo(BigDecimal.ZERO) > 0) {
            PagoVentaRequest pagoAnticipo = new PagoVentaRequest();
            pagoAnticipo.setMonto(anticipo);
            pagoAnticipo.setMetodoPago(
                    request.getMetodoPagoAnticipo() != null && !request.getMetodoPagoAnticipo().isBlank()
                            ? request.getMetodoPagoAnticipo()
                            : "EFECTIVO"
            );
            pagoAnticipo.setCuentaId(request.getCuentaIdAnticipo());
            pagoAnticipo.setObservaciones("Anticipo crédito personal");
            pagos.add(pagoAnticipo);
        }

        PagoVentaRequest pagoCredito = new PagoVentaRequest();
        pagoCredito.setMonto(plan.montoFinanciado);
        pagoCredito.setMetodoPago("CREDITO");
        pagoCredito.setCreditoPlazoMeses(request.getCreditoPlazoMeses());
        pagoCredito.setCreditoTasaInteres(tasa);
        pagoCredito.setCreditoDescripcion(request.getCreditoDescripcion());
        pagoCredito.setCreditoMontoAnticipo(anticipo);
        pagoCredito.setCreditoModoDistribucion(plan.modoDistribucion);
        pagos.add(pagoCredito);

        internalRequest.setPagos(pagos);
        return registrarVenta(internalRequest);
    }

    private BigDecimal resolverSubtotalSimulacion(CreditoSimulacionRequest request) {
        if (request.getMontoTotal() != null && request.getMontoTotal().compareTo(BigDecimal.ZERO) > 0) {
            return request.getMontoTotal();
        }
        VentaCreditoPersonalRequest tmp = new VentaCreditoPersonalRequest();
        tmp.setDetalles(request.getDetalles());
        return calcularTotalCreditoPersonal(tmp);
    }

    private BigDecimal calcularTotalCreditoPersonal(VentaCreditoPersonalRequest request) {
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos un detalle de venta para crédito personal");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (var detalleReq : request.getDetalles()) {
            VarianteArticulo variante = null;
            Articulo articulo = null;

            if (detalleReq.getVarianteId() != null) {
                Long varianteId = detalleReq.getVarianteId();
                variante = varianteArticuloRepository.findById(varianteId)
                        .orElseThrow(() -> new RuntimeException("Variante no encontrada con ID: " + varianteId));
                Long articuloId = variante.getArticuloId();
                articulo = articuloRepository.findById(articuloId)
                        .orElseThrow(() -> new RuntimeException("Artículo de la variante no encontrado con ID: " + articuloId));
            } else {
                if (detalleReq.getArticuloId() == null) {
                    throw new RuntimeException("Cada detalle requiere articuloId o varianteId");
                }
                Long articuloId = detalleReq.getArticuloId();
                articulo = articuloRepository.findById(articuloId)
                        .orElseThrow(() -> new RuntimeException("Artículo no encontrado con ID: " + articuloId));
            }

            if (detalleReq.getCantidad() == null || detalleReq.getCantidad() <= 0) {
                throw new RuntimeException("La cantidad del detalle debe ser mayor a cero");
            }

            BigDecimal precioUnitario;
            if (variante != null) {
                precioUnitario = obtenerPrecioUnitarioDesdeVariante(variante);
                if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("No existe precio unitario válido para la variante con ID: " + variante.getId());
                }
            } else {
                if (detalleReq.getPrecioUnitario() == null || detalleReq.getPrecioUnitario().compareTo(BigDecimal.ZERO) < 0) {
                    throw new RuntimeException("El precio unitario del detalle debe ser un valor válido");
                }
                precioUnitario = detalleReq.getPrecioUnitario();
            }

            BigDecimal descuentoMonto = detalleReq.getDescuentoMonto() != null ? detalleReq.getDescuentoMonto() : BigDecimal.ZERO;
            BigDecimal impuesto = detalleReq.getImpuesto() != null ? detalleReq.getImpuesto() : BigDecimal.ZERO;
            BigDecimal detalleSubtotal = precioUnitario.multiply(BigDecimal.valueOf(detalleReq.getCantidad())).subtract(descuentoMonto);
            BigDecimal detalleTotal = detalleSubtotal.add(impuesto);
            total = total.add(detalleTotal);
        }

        return total;
    }

    @Transactional(readOnly = true)
    public List<CajaCuentaResponse> listarCajas() {
        return cuentaRepository.findByTipoAndActivoTrue(CuentaFinanciera.TipoCuenta.CAJA)
                .stream()
                .map(this::mapCajaCuentaResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CajaMovimientoResponse> listarMovimientosCaja() {
        return movimientoFinancieroRepository.findByCuentaTipo(CuentaFinanciera.TipoCuenta.CAJA)
                .stream()
                .map(this::mapCajaMovimientoResponse)
                .collect(Collectors.toList());
    }

    private Stock findStock(Long articuloId, Long varianteId, Long sucursalId) {
        return varianteId != null
                ? stockRepository.findByArticuloIdAndVarianteIdAndSucursalId(articuloId, varianteId, sucursalId)
                .orElseThrow(() -> new RuntimeException("Stock no encontrado para el artículo/variante en la sucursal"))
                : stockRepository.findByArticuloIdAndSucursalId(articuloId, sucursalId)
                .orElseThrow(() -> new RuntimeException("Stock no encontrado para el artículo en la sucursal"));
    }

    private Stock findStockByVariante(Long varianteId, Long sucursalId) {
        return stockRepository.findByVarianteIdAndSucursalId(varianteId, sucursalId)
                .orElseThrow(() -> new RuntimeException("Stock no encontrado para la variante en la sucursal"));
    }

    private BigDecimal obtenerPrecioUnitarioDesdeVariante(VarianteArticulo variante) {
        if (variante == null) {
            return null;
        }
        if (variante.getHistorialPrecios() != null && !variante.getHistorialPrecios().isEmpty()) {
            return variante.getHistorialPrecios().stream()
                    .max((a, b) -> a.getFecha().compareTo(b.getFecha()))
                    .map(historialPrecio -> historialPrecio.getPrecioNuevo())
                    .orElse(null);
        }
        if (variante.getListaPrecio() != null && variante.getListaPrecio().getPrecio() != null) {
            return variante.getListaPrecio().getPrecio();
        }
        return null;
    }

    private void ajustarStock(Stock stock, Integer cantidad, String referencia) {
        Integer disponibleAnterior = stock.getCantidadDisponible();
        if (disponibleAnterior == null) {
            disponibleAnterior = 0;
        }
        if (disponibleAnterior < cantidad) {
            throw new RuntimeException("No hay stock suficiente para el artículo " + stock.getArticulo().getId());
        }

        Integer nuevoDisponible = disponibleAnterior - cantidad;
        stock.setCantidadDisponible(nuevoDisponible);
        stock.setCantidadActual(Math.max(0, stock.getCantidadActual() - cantidad));
        stockRepository.save(stock);

        StockMovimiento movimiento = new StockMovimiento();
        movimiento.setStock(stock);
        movimiento.setTipo(StockMovimiento.TipoMovimiento.SALIDA_VENTA);
        movimiento.setCantidad(cantidad);
        movimiento.setSaldoAnterior(disponibleAnterior);
        movimiento.setSaldoNuevo(nuevoDisponible);
        movimiento.setReferencia(referencia);
        movimiento.setDescripcion("Salida por venta");
        movimiento.setUsuario("sistema");

        stockMovimientoRepository.save(movimiento);
    }

    private void registrarMovimientoCaja(CuentaFinanciera cuenta, PagoVenta pago, String referenciaVenta) {
        BigDecimal saldoAnterior = cuenta.getSaldoActual() != null ? cuenta.getSaldoActual() : BigDecimal.ZERO;
        BigDecimal saldoNuevo = saldoAnterior.add(pago.getMonto());
        cuenta.setSaldoActual(saldoNuevo);
        cuentaRepository.save(cuenta);

        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setCuenta(cuenta);
        movimiento.setNumero("MV-" + UUID.randomUUID().toString().replace("-", ""));
        movimiento.setTipo(MovimientoFinanciero.TipoMovimiento.INGRESO);
        movimiento.setMonto(pago.getMonto());
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoNuevo(saldoNuevo);
        movimiento.setDescripcion("Pago de venta " + referenciaVenta);
        movimiento.setReferencia(pago.getReferencia());
        movimiento.setResponsable("sistema");

        movimientoFinancieroRepository.save(movimiento);
    }

    private Cuenta crearOEncontrarCuentaCredito(Cliente cliente, Sucursal sucursal) {
        return creditoCuentaRepository.findByClienteIdAndSucursalIdAndActivoTrue(cliente.getId(), sucursal.getId())
                .orElseGet(() -> {
                    Cuenta cuenta = new Cuenta();
                    cuenta.setCliente(cliente);
                    cuenta.setSucursal(sucursal);
                    cuenta.setNumero("CC-" + cliente.getId() + "-" + sucursal.getId() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
                    cuenta.setActivo(true);
                    cuenta.setSaldoActual(BigDecimal.ZERO);
                    cuenta.setLimiteCredito(BigDecimal.ZERO);
                    return creditoCuentaRepository.save(cuenta);
                });
    }

    private VentaResponse mapVentaResponse(Venta venta) {
        VentaResponse response = new VentaResponse();
        response.setId(venta.getId());
        response.setClienteId(venta.getCliente().getId());
        response.setClienteDni(venta.getCliente().getDni());
        response.setEmpleadoId(venta.getEmpleado() != null ? venta.getEmpleado().getId() : null);
        response.setSucursalId(venta.getSucursal().getId());
        response.setNumeroFactura(venta.getNumeroFactura());
        response.setFechaVenta(venta.getFechaVenta());
        response.setSubtotal(venta.getSubtotal());
        response.setDescuento(venta.getDescuento());
        response.setImpuesto(venta.getImpuesto());
        response.setTotal(venta.getTotal());
        response.setEstado(venta.getEstado() != null ? venta.getEstado().name() : null);
        response.setMetodoPago(venta.getMetodoPago());
        response.setObservaciones(venta.getObservaciones());
        response.setDetalles(venta.getDetalles().stream().map(this::mapVentaDetalleResponse).collect(Collectors.toList()));
        response.setPagos(venta.getPagos().stream().map(this::mapPagoVentaResponse).collect(Collectors.toList()));
        return response;
    }

    private VentaDetalleResponse mapVentaDetalleResponse(VentaDetalle detalle) {
        VentaDetalleResponse response = new VentaDetalleResponse();
        response.setId(detalle.getId());
        response.setArticuloId(detalle.getArticulo().getId());
        response.setVarianteId(detalle.getVariante() != null ? detalle.getVariante().getId() : null);
        response.setCantidad(detalle.getCantidad());
        response.setPrecioUnitario(detalle.getPrecioUnitario());
        response.setDescuentoPorcentaje(detalle.getDescuentoPorcentaje());
        response.setDescuentoMonto(detalle.getDescuentoMonto());
        response.setSubtotal(detalle.getSubtotal());
        response.setImpuesto(detalle.getImpuesto());
        response.setTotal(detalle.getTotal());
        response.setLote(detalle.getLote());
        response.setNumeroSerie(detalle.getNumeroSerie());
        return response;
    }

    private PagoVentaResponse mapPagoVentaResponse(PagoVenta pago) {
        PagoVentaResponse response = new PagoVentaResponse();
        response.setId(pago.getId());
        response.setCuentaId(pago.getCuenta() != null ? pago.getCuenta().getId() : null);
        response.setMonto(pago.getMonto());
        response.setMetodoPago(pago.getMetodoPago());
        response.setNumero(pago.getNumero());
        response.setReferencia(pago.getReferencia());
        response.setNumeroComprobante(pago.getNumeroComprobante());
        response.setObservaciones(pago.getObservaciones());
        response.setEstado(pago.getEstado() != null ? pago.getEstado().name() : null);
        response.setCreatedAt(pago.getCreatedAt());
        return response;
    }

    private CajaCuentaResponse mapCajaCuentaResponse(CuentaFinanciera cuenta) {
        CajaCuentaResponse response = new CajaCuentaResponse();
        response.setId(cuenta.getId());
        response.setNombre(cuenta.getNombre());
        response.setNumero(cuenta.getNumero());
        response.setTipo(cuenta.getTipo() != null ? cuenta.getTipo().name() : null);
        response.setSaldoActual(cuenta.getSaldoActual());
        return response;
    }

    /** base + 1 mes = primera cuota; si no hay fecha, primera cuota a ~30 días. */
    private LocalDateTime resolverBaseFechaCredito(LocalDate fechaPrimerVencimiento) {
        if (fechaPrimerVencimiento != null) {
            return fechaPrimerVencimiento.minusMonths(1).atStartOfDay();
        }
        return LocalDateTime.now();
    }

    private CajaMovimientoResponse mapCajaMovimientoResponse(MovimientoFinanciero movimiento) {
        CajaMovimientoResponse response = new CajaMovimientoResponse();
        response.setId(movimiento.getId());
        response.setCuentaId(movimiento.getCuenta().getId());
        response.setCuentaNombre(movimiento.getCuenta().getNombre());
        response.setNumero(movimiento.getNumero());
        response.setTipo(movimiento.getTipo() != null ? movimiento.getTipo().name() : null);
        response.setMonto(movimiento.getMonto());
        response.setSaldoAnterior(movimiento.getSaldoAnterior());
        response.setSaldoNuevo(movimiento.getSaldoNuevo());
        response.setDescripcion(movimiento.getDescripcion());
        response.setReferencia(movimiento.getReferencia());
        response.setResponsable(movimiento.getResponsable());
        response.setCreatedAt(movimiento.getCreatedAt());
        return response;
    }
}
