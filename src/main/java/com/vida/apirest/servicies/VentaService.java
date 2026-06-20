package com.vida.apirest.servicies;

import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.dto.venta.CajaCuentaResponse;
import com.vida.apirest.dto.venta.CajaMovimientoResponse;
import com.vida.apirest.dto.venta.CreditoSimulacionRequest;
import com.vida.apirest.dto.venta.CreditoSimulacionResponse;
import com.vida.apirest.dto.venta.PagoVentaRequest;
import com.vida.apirest.dto.venta.PagoVentaResponse;
import com.vida.apirest.dto.venta.VentaCancelarRequest;
import com.vida.apirest.dto.venta.VentaCambioArticuloRequest;
import com.vida.apirest.dto.venta.VentaCambioArticuloResponse;
import com.vida.apirest.dto.venta.VentaCreateRequest;
import com.vida.apirest.dto.venta.VentaCreditoPersonalRequest;
import com.vida.apirest.dto.venta.VentaDetalleResponse;
import com.vida.apirest.dto.venta.VentaHistorialItemResponse;
import com.vida.apirest.dto.venta.VentaResponse;
import com.vida.apirest.model.venta.VentaCambioArticulo;
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
import com.vida.apirest.repositories.StockRepository;
import com.vida.apirest.repositories.VentaCambioArticuloRepository;
import com.vida.apirest.repositories.VentaRepository;
import com.vida.apirest.repositories.VarianteArticuloRepository;
import com.vida.apirest.security.SucursalScopeService;
import com.vida.apirest.dto.afip.FacturaAFIPResponse;
import com.vida.apirest.model.afip.FacturaAFIP;
import com.vida.apirest.servicies.afip.FacturaAFIPService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private static final Logger log = LoggerFactory.getLogger(VentaService.class);

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ArticuloRepository articuloRepository;
    private final VarianteArticuloRepository varianteArticuloRepository;
    private final StockRepository stockRepository;
    private final StockOperacionesService stockOperacionesService;
    private final SucursalRepository sucursalRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final FinanzasCuentaFinancieraRepository cuentaRepository;
    private final CuentaRepository creditoCuentaRepository;
    private final CreditoRepository creditoRepository;
    private final MovimientoFinancieroRepository movimientoFinancieroRepository;
    private final VentaCambioArticuloRepository ventaCambioArticuloRepository;
    private final FacturaAFIPService facturaAFIPService;
    private final CajaMovimientoService cajaMovimientoService;
    private final VentaDetalleSupport ventaDetalleSupport;
    private final SucursalScopeService sucursalScopeService;

    @Transactional
    public VentaResponse registrarVenta(VentaCreateRequest request) {
        return registrarVenta(request, true);
    }

    @Transactional
    public VentaResponse registrarVenta(VentaCreateRequest request, boolean descontarStock) {
        validarRegistroVenta(request);

        Cliente cliente = cargarCliente(request.getClienteDni());
        Sucursal sucursal = cargarSucursal(request.getSucursalId());
        Empleado empleado = cargarEmpleadoOpcional(request.getEmpleadoId());

        Venta venta = inicializarVentaCabecera(request, cliente, sucursal, empleado);
        TotalesAcumulados totales = agregarDetallesAVenta(venta, request.getDetalles(), sucursal, descontarStock);
        aplicarTotalesVenta(venta, totales);
        venta.setEstado(tienePagos(request) ? Venta.EstadoVenta.CONFIRMADA : Venta.EstadoVenta.BORRADOR);

        Venta ventaGuardada = ventaRepository.save(venta);
        if (tienePagos(request)) {
            procesarPagosVenta(ventaGuardada, request.getPagos(), cliente, sucursal);
        }

        return construirRespuestaVenta(ventaGuardada.getId(), request);
    }

    private void validarRegistroVenta(VentaCreateRequest request) {
        if (request.getClienteDni() == null || request.getClienteDni().isBlank()) {
            throw new RuntimeException("DNI de cliente requerido para registrar la venta");
        }
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos un detalle de venta");
        }
        if (request.getSucursalId() == null) {
            throw new RuntimeException("Sucursal requerida para registrar la venta");
        }
    }

    private Cliente cargarCliente(String dni) {
        return clienteRepository.findByDni(dni)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con DNI: " + dni));
    }

    private Sucursal cargarSucursal(Long sucursalId) {
        sucursalScopeService.assertCanUse(sucursalId);
        return sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con ID: " + sucursalId));
    }

    private Empleado cargarEmpleadoOpcional(Long empleadoId) {
        if (empleadoId == null) {
            return null;
        }
        return empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + empleadoId));
    }

    private Venta inicializarVentaCabecera(
            VentaCreateRequest request,
            Cliente cliente,
            Sucursal sucursal,
            Empleado empleado
    ) {
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
        return venta;
    }

    private TotalesAcumulados agregarDetallesAVenta(
            Venta venta,
            List<com.vida.apirest.dto.venta.VentaDetalleRequest> detalles,
            Sucursal sucursal,
            boolean descontarStock
    ) {
        TotalesAcumulados totales = new TotalesAcumulados();
        for (var detalleReq : detalles) {
            VentaDetalleSupport.ArticuloVarianteResuelto resolucion = ventaDetalleSupport.resolverArticuloYVariante(detalleReq);
            ventaDetalleSupport.validarCantidad(detalleReq.getCantidad());

            BigDecimal precioUnitario = ventaDetalleSupport.resolverPrecioUnitario(detalleReq, resolucion.variante());
            if (descontarStock) {
                descontarStockDetalle(resolucion, detalleReq.getCantidad(), sucursal.getId(), venta.getNumeroFactura());
            }

            VentaDetalleSupport.MontosDetalle montos = ventaDetalleSupport.calcularMontos(precioUnitario, detalleReq);
            VentaDetalle detalle = ventaDetalleSupport.construirDetalle(venta, resolucion, detalleReq, precioUnitario, montos);
            totales.acumular(montos);
            venta.getDetalles().add(detalle);
        }
        return totales;
    }

    private void descontarStockDetalle(
            VentaDetalleSupport.ArticuloVarianteResuelto resolucion,
            Integer cantidad,
            Long sucursalId,
            String referencia
    ) {
        Stock stock = resolucion.variante() != null
                ? stockOperacionesService.requireStockByVariante(resolucion.variante().getId(), sucursalId)
                : stockOperacionesService.requireStock(resolucion.articulo().getId(), null, sucursalId);
        ajustarStock(stock, cantidad, referencia);
    }

    private void aplicarTotalesVenta(Venta venta, TotalesAcumulados totales) {
        venta.setSubtotal(totales.subtotal);
        venta.setDescuento(totales.descuento);
        venta.setImpuesto(totales.impuesto);
        venta.setTotal(totales.total);
    }

    private boolean tienePagos(VentaCreateRequest request) {
        return request.getPagos() != null && !request.getPagos().isEmpty();
    }

    private void procesarPagosVenta(Venta ventaGuardada, List<PagoVentaRequest> pagos, Cliente cliente, Sucursal sucursal) {
        for (PagoVentaRequest pagoReq : pagos) {
            if (pagoReq.getMonto() == null || pagoReq.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Cada pago debe tener un monto mayor a cero");
            }
            if (esPagoCredito(pagoReq)) {
                registrarPagoCredito(ventaGuardada, pagoReq, cliente, sucursal);
            } else {
                registrarPagoEfectivoOUOtro(ventaGuardada, pagoReq);
            }
        }
    }

    private boolean esPagoCredito(PagoVentaRequest pagoReq) {
        return pagoReq.getMetodoPago() != null && pagoReq.getMetodoPago().equalsIgnoreCase("CREDITO");
    }

    private void registrarPagoCredito(Venta ventaGuardada, PagoVentaRequest pagoReq, Cliente cliente, Sucursal sucursal) {
        if (pagoReq.getCreditoPlazoMeses() == null || pagoReq.getCreditoPlazoMeses() <= 0) {
            throw new RuntimeException("Para pagos con crédito se requiere un plazo en meses mayor a cero");
        }

        Cuenta cuentaCredito = crearOEncontrarCuentaCredito(cliente, sucursal);
        Credito credito = construirCreditoDesdePago(ventaGuardada, pagoReq, cliente, sucursal);
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
        creditoRepository.save(credito);

        BigDecimal saldoAnteriorCredito = cuentaCredito.getSaldoActual() != null ? cuentaCredito.getSaldoActual() : BigDecimal.ZERO;
        cuentaCredito.setSaldoActual(saldoAnteriorCredito.add(plan.montoFinanciado));
        creditoCuentaRepository.save(cuentaCredito);

        PagoVenta pago = crearPagoBase(ventaGuardada, pagoReq);
        pago.setMonto(plan.montoFinanciado);
        pago.setEstado(PagoVenta.EstadoPago.PENDIENTE);
        pago.setObservaciones((pago.getObservaciones() != null ? pago.getObservaciones() + " " : "")
                + "Crédito generado: " + credito.getNumero());
        pagoVentaRepository.save(pago);
        ventaGuardada.getPagos().add(pago);
    }

    private Credito construirCreditoDesdePago(
            Venta ventaGuardada,
            PagoVentaRequest pagoReq,
            Cliente cliente,
            Sucursal sucursal
    ) {
        Credito credito = new Credito();
        credito.setCliente(cliente);
        credito.setSucursal(sucursal);
        credito.setVenta(ventaGuardada);
        credito.setNumero("CR-" + cliente.getId() + "-" + sucursal.getId() + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        credito.setPlazoMeses(pagoReq.getCreditoPlazoMeses());
        credito.setTasaInteres(pagoReq.getCreditoTasaInteres() != null ? pagoReq.getCreditoTasaInteres() : BigDecimal.ZERO);
        credito.setDescripcion(pagoReq.getCreditoDescripcion());
        credito.setEstado(Credito.EstadoCredito.ACTIVO);
        return credito;
    }

    private void registrarPagoEfectivoOUOtro(Venta ventaGuardada, PagoVentaRequest pagoReq) {
        PagoVenta pago = crearPagoBase(ventaGuardada, pagoReq);
        pago.setEstado(PagoVenta.EstadoPago.RECIBIDO);

        CuentaFinanciera cuenta = resolverCuentaFinancieraPago(pagoReq);
        if (cuenta == null) {
            pago.setEstado(PagoVenta.EstadoPago.PENDIENTE);
            pagoVentaRepository.save(pago);
            ventaGuardada.getPagos().add(pago);
            return;
        }

        pago.setCuenta(cuenta);
        pagoVentaRepository.save(pago);
        ventaGuardada.getPagos().add(pago);
        registrarMovimientoCaja(cuenta, pago, ventaGuardada.getNumeroFactura());
    }

    private CuentaFinanciera resolverCuentaFinancieraPago(PagoVentaRequest pagoReq) {
        if (pagoReq.getCuentaId() != null) {
            return cuentaRepository.findById(pagoReq.getCuentaId())
                    .orElseThrow(() -> new RuntimeException("Cuenta financiera no encontrada con ID: " + pagoReq.getCuentaId()));
        }
        return cuentaRepository.findFirstByTipoAndActivoTrue(CuentaFinanciera.TipoCuenta.CAJA).orElse(null);
    }

    private PagoVenta crearPagoBase(Venta ventaGuardada, PagoVentaRequest pagoReq) {
        PagoVenta pago = new PagoVenta();
        pago.setVenta(ventaGuardada);
        pago.setMonto(pagoReq.getMonto());
        pago.setMetodoPago(pagoReq.getMetodoPago());
        pago.setReferencia(pagoReq.getReferencia());
        pago.setNumeroComprobante(pagoReq.getNumeroComprobante());
        pago.setObservaciones(pagoReq.getObservaciones());
        pago.setNumero("PV-" + UUID.randomUUID().toString().replace("-", ""));
        return pago;
    }

    private VentaResponse construirRespuestaVenta(Long ventaId, VentaCreateRequest request) {
        Venta ventaCompleta = ventaRepository.findByIdWithDetalles(ventaId)
                .orElseThrow(() -> new RuntimeException("Error al recuperar la venta registrada"));
        FacturaAFIP facturaArca = facturaAFIPService.intentarFacturarVenta(ventaCompleta.getId(), request.getFacturaAfip());

        VentaResponse response = mapVentaResponse(ventaCompleta);
        if (facturaArca != null && facturaArca.getIdFacturaAFIP() != null) {
            adjuntarDetalleAfipSiDisponible(response, facturaArca.getIdFacturaAFIP(), ventaCompleta.getId());
        }
        return response;
    }

    private void adjuntarDetalleAfipSiDisponible(VentaResponse response, Long idFacturaAfip, Long ventaId) {
        try {
            response.setFacturaAfip(facturaAFIPService.obtenerDetalle(idFacturaAfip));
        } catch (Exception e) {
            log.debug("Detalle AFIP no adjunto en venta {}: {}", ventaId, e.getMessage());
        }
    }

    private static final class TotalesAcumulados {
        private BigDecimal subtotal = BigDecimal.ZERO;
        private BigDecimal descuento = BigDecimal.ZERO;
        private BigDecimal impuesto = BigDecimal.ZERO;
        private BigDecimal total = BigDecimal.ZERO;

        private void acumular(VentaDetalleSupport.MontosDetalle montos) {
            subtotal = subtotal.add(montos.subtotal());
            descuento = descuento.add(montos.descuentoMonto());
            impuesto = impuesto.add(montos.impuesto());
            total = total.add(montos.total());
        }
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
        return registrarVentaCreditoPersonal(request, true);
    }

    @Transactional
    public VentaResponse registrarVentaCreditoPersonal(VentaCreditoPersonalRequest request, boolean descontarStock) {
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
        return registrarVenta(internalRequest, descontarStock);
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
            VentaDetalleSupport.ArticuloVarianteResuelto resolucion = ventaDetalleSupport.resolverArticuloYVariante(detalleReq);
            ventaDetalleSupport.validarCantidad(detalleReq.getCantidad());
            BigDecimal precioUnitario = ventaDetalleSupport.resolverPrecioUnitario(detalleReq, resolucion.variante());
            VentaDetalleSupport.MontosDetalle montos = ventaDetalleSupport.calcularMontos(precioUnitario, detalleReq);
            total = total.add(montos.total());
        }
        return total;
    }

    @Transactional(readOnly = true)
    public PageResponse<VentaHistorialItemResponse> listarHistorial(
            Long sucursalId,
            String estado,
            LocalDate desde,
            LocalDate hasta,
            String q,
            int page,
            int size
    ) {
        LocalDateTime desdeDt = (desde != null ? desde : LocalDate.now()).atStartOfDay();
        LocalDate hastaInclusive = hasta != null ? hasta : LocalDate.now();
        LocalDateTime hastaExclusivo = hastaInclusive.plusDays(1).atStartOfDay();

        String estadoFilter = (estado == null || estado.isBlank()) ? null : estado.trim().toUpperCase();
        Long scopedSucursalId = sucursalScopeService.enforceFilter(sucursalId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Venta> result = ventaRepository.searchHistorial(
                scopedSucursalId,
                estadoFilter,
                desdeDt,
                hastaExclusivo,
                q != null ? q.trim() : null,
                pageable
        );
        return PageResponse.from(result.map(this::mapHistorialItem));
    }

    @Transactional(readOnly = true)
    public VentaResponse obtenerVenta(Long id) {
        Venta venta = ventaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));
        sucursalScopeService.assertCanAccess(venta.getSucursal().getId());
        return mapVentaResponseCompleto(venta);
    }

    @Transactional
    public VentaResponse cancelarVenta(Long id, VentaCancelarRequest request) {
        if (request == null || request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new RuntimeException("Debe indicar el motivo de cancelación");
        }

        Venta venta = ventaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));
        sucursalScopeService.assertCanAccess(venta.getSucursal().getId());

        if (venta.getEstado() == Venta.EstadoVenta.CANCELADA) {
            throw new RuntimeException("La venta ya está cancelada");
        }
        if (venta.getEstado() == Venta.EstadoVenta.BORRADOR) {
            throw new RuntimeException("No se puede cancelar una venta en borrador");
        }

        Long sucursalId = venta.getSucursal().getId();
        String referencia = venta.getNumeroFactura();

        for (VentaDetalle detalle : venta.getDetalles()) {
            if (detalle.getVariante() != null) {
                Stock stock = stockOperacionesService.requireStockByVariante(detalle.getVariante().getId(), sucursalId);
                ingresarStockDevolucion(stock, detalle.getCantidad(), referencia);
            } else if (detalle.getArticulo() != null) {
                Stock stock = stockOperacionesService.requireStock(detalle.getArticulo().getId(), null, sucursalId);
                ingresarStockDevolucion(stock, detalle.getCantidad(), referencia);
            }
        }

        if (venta.getPagos() != null) {
            for (PagoVenta pago : venta.getPagos()) {
                if (pago.getEstado() == PagoVenta.EstadoPago.RECIBIDO && pago.getCuenta() != null) {
                    revertirMovimientoCaja(pago.getCuenta(), pago, referencia);
                    pago.setEstado(PagoVenta.EstadoPago.DEVUELTO);
                    pagoVentaRepository.save(pago);
                } else if (pago.getEstado() == PagoVenta.EstadoPago.PENDIENTE
                        && pago.getMetodoPago() != null
                        && pago.getMetodoPago().equalsIgnoreCase("CREDITO")) {
                    pago.setEstado(PagoVenta.EstadoPago.DEVUELTO);
                    pagoVentaRepository.save(pago);
                }
            }
        }

        cancelarCreditosVenta(venta);

        venta.setEstado(Venta.EstadoVenta.CANCELADA);
        venta.setMotivoCancelacion(request.getMotivo().trim());
        venta.setFechaCancelacion(LocalDateTime.now());
        ventaRepository.save(venta);

        return mapVentaResponseCompleto(ventaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Error al recuperar la venta cancelada")));
    }

    @Transactional
    public VentaResponse cambiarArticulo(Long ventaId, VentaCambioArticuloRequest request) {
        if (request == null || request.getVentaDetalleId() == null) {
            throw new RuntimeException("Debe indicar la línea de venta a cambiar");
        }
        if (request.getNuevaVarianteId() == null) {
            throw new RuntimeException("Debe indicar la variante nueva");
        }
        if (request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new RuntimeException("Debe indicar el motivo del cambio");
        }

        Venta venta = ventaRepository.findByIdWithDetalles(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + ventaId));
        sucursalScopeService.assertCanAccess(venta.getSucursal().getId());

        if (venta.getEstado() == Venta.EstadoVenta.CANCELADA) {
            throw new RuntimeException("No se puede cambiar artículos en una venta cancelada");
        }
        if (venta.getEstado() == Venta.EstadoVenta.BORRADOR) {
            throw new RuntimeException("La venta debe estar confirmada para registrar un cambio");
        }

        VentaDetalle detalle = venta.getDetalles().stream()
                .filter(d -> d.getId().equals(request.getVentaDetalleId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Línea de venta no encontrada en esta venta"));

        VarianteArticulo varianteDevuelta = detalle.getVariante();
        VarianteArticulo varianteNueva = varianteArticuloRepository.findById(request.getNuevaVarianteId())
                .orElseThrow(() -> new RuntimeException("Variante nueva no encontrada"));

        if (varianteDevuelta != null && varianteNueva.getId().equals(varianteDevuelta.getId())) {
            throw new RuntimeException("El artículo nuevo debe ser distinto al devuelto");
        }

        int cantidad = request.getCantidad() != null ? request.getCantidad() : detalle.getCantidad();
        if (cantidad <= 0 || cantidad > detalle.getCantidad()) {
            throw new RuntimeException("Cantidad inválida para el cambio");
        }

        Long sucursalId = venta.getSucursal().getId();
        String referencia = venta.getNumeroFactura();

        if (varianteDevuelta != null) {
            Stock stockDevuelto = stockOperacionesService.requireStockByVariante(varianteDevuelta.getId(), sucursalId);
            ingresarStockDevolucion(stockDevuelto, cantidad, referencia);
        } else {
            Stock stockDevuelto = stockOperacionesService.requireStock(detalle.getArticulo().getId(), null, sucursalId);
            ingresarStockDevolucion(stockDevuelto, cantidad, referencia);
        }

        Stock stockNuevo = stockOperacionesService.requireStockByVariante(varianteNueva.getId(), sucursalId);
        ajustarStock(stockNuevo, cantidad, referencia + "-CAMBIO");

        Articulo articuloNuevo = articuloRepository.findById(varianteNueva.getArticuloId())
                .orElseThrow(() -> new RuntimeException("Artículo de la variante nueva no encontrado"));

        BigDecimal precioAnterior = detalle.getPrecioUnitario();
        BigDecimal precioNuevo = ventaDetalleSupport.obtenerPrecioUnitarioDesdeVariante(varianteNueva);
        if (precioNuevo == null || precioNuevo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("No existe precio válido para la variante nueva");
        }

        BigDecimal diferencia = precioNuevo.subtract(precioAnterior).multiply(BigDecimal.valueOf(cantidad));

        VentaCambioArticulo cambio = new VentaCambioArticulo();
        cambio.setVenta(venta);
        cambio.setVentaDetalle(detalle);
        cambio.setVarianteDevuelta(varianteDevuelta);
        cambio.setVarianteNueva(varianteNueva);
        cambio.setCantidad(cantidad);
        cambio.setMotivo(request.getMotivo().trim());
        cambio.setPrecioAnterior(precioAnterior);
        cambio.setPrecioNuevo(precioNuevo);
        cambio.setDiferenciaPrecio(diferencia);
        cambio.setObservaciones(request.getObservaciones());
        ventaCambioArticuloRepository.save(cambio);

        detalle.setArticulo(articuloNuevo);
        detalle.setVariante(varianteNueva);
        detalle.setPrecioUnitario(precioNuevo);
        BigDecimal detalleSubtotal = precioNuevo.multiply(BigDecimal.valueOf(detalle.getCantidad()))
                .subtract(detalle.getDescuentoMonto() != null ? detalle.getDescuentoMonto() : BigDecimal.ZERO);
        BigDecimal detalleTotal = detalleSubtotal.add(detalle.getImpuesto() != null ? detalle.getImpuesto() : BigDecimal.ZERO);
        detalle.setSubtotal(detalleSubtotal);
        detalle.setTotal(detalleTotal);

        recalcularTotalesVenta(venta);
        ventaRepository.save(venta);

        return mapVentaResponseCompleto(ventaRepository.findByIdWithDetalles(ventaId)
                .orElseThrow(() -> new RuntimeException("Error al recuperar la venta actualizada")));
    }

    @Transactional(readOnly = true)
    public List<CajaCuentaResponse> listarCajas() {
        return cuentaRepository.findByTipoAndActivoTrue(CuentaFinanciera.TipoCuenta.CAJA)
                .stream()
                .map(this::mapCajaCuentaResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CajaMovimientoResponse> listarMovimientosCaja(Long cuentaId) {
        return movimientoFinancieroRepository
                .findByCuentaTipoOrderByCreatedAtDesc(CuentaFinanciera.TipoCuenta.CAJA)
                .stream()
                .filter(m -> cuentaId == null
                        || (m.getCuenta() != null && cuentaId.equals(m.getCuenta().getId())))
                .map(this::mapCajaMovimientoResponse)
                .collect(Collectors.toList());
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

        stockOperacionesService.registrarMovimiento(
                stock,
                StockMovimiento.TipoMovimiento.SALIDA_VENTA,
                cantidad,
                disponibleAnterior,
                nuevoDisponible,
                referencia,
                "Salida por venta"
        );
    }

    private void registrarMovimientoCaja(CuentaFinanciera cuenta, PagoVenta pago, String referenciaVenta) {
        cajaMovimientoService.registrarIngreso(
                cuenta,
                pago.getMonto(),
                "Pago de venta " + referenciaVenta,
                pago.getReferencia());
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

    private void ingresarStockDevolucion(Stock stock, Integer cantidad, String referencia) {
        Integer disponibleAnterior = stock.getCantidadDisponible() != null ? stock.getCantidadDisponible() : 0;
        Integer nuevoDisponible = disponibleAnterior + cantidad;
        stock.setCantidadDisponible(nuevoDisponible);
        stock.setCantidadActual((stock.getCantidadActual() != null ? stock.getCantidadActual() : 0) + cantidad);
        stockRepository.save(stock);

        stockOperacionesService.registrarMovimiento(
                stock,
                StockMovimiento.TipoMovimiento.INGRESO_DEVOLUCION,
                cantidad,
                disponibleAnterior,
                nuevoDisponible,
                referencia,
                "Ingreso por devolución o cambio"
        );
    }

    private void revertirMovimientoCaja(CuentaFinanciera cuenta, PagoVenta pago, String referenciaVenta) {
        cajaMovimientoService.registrarEgreso(
                cuenta,
                pago.getMonto(),
                "Reversión por cancelación de venta " + referenciaVenta,
                pago.getReferencia());
    }

    private void cancelarCreditosVenta(Venta venta) {
        List<Credito> creditos = creditoRepository.findByVentaId(venta.getId());
        if (creditos.isEmpty()) {
            return;
        }
        Cuenta cuentaCredito = creditoCuentaRepository
                .findByClienteIdAndSucursalIdAndActivoTrue(venta.getCliente().getId(), venta.getSucursal().getId())
                .orElse(null);

        for (Credito credito : creditos) {
            if (credito.getEstado() == Credito.EstadoCredito.CANCELADO
                    || credito.getEstado() == Credito.EstadoCredito.PAGADO) {
                continue;
            }
            BigDecimal saldoCredito = credito.getSaldo() != null ? credito.getSaldo() : credito.getImporte();
            if (cuentaCredito != null && saldoCredito.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal saldoCuenta = cuentaCredito.getSaldoActual() != null ? cuentaCredito.getSaldoActual() : BigDecimal.ZERO;
                cuentaCredito.setSaldoActual(saldoCuenta.subtract(saldoCredito).max(BigDecimal.ZERO));
            }
            credito.setEstado(Credito.EstadoCredito.CANCELADO);
            credito.setSaldo(BigDecimal.ZERO);
            if (credito.getCuotas() != null) {
                credito.getCuotas().forEach(cuota -> {
                    if (cuota.getEstado() != Cuota.EstadoCuota.PAGADA) {
                        cuota.setEstado(Cuota.EstadoCuota.CANCELADA);
                        cuota.setSaldo(BigDecimal.ZERO);
                    }
                });
            }
            creditoRepository.save(credito);
        }
        if (cuentaCredito != null) {
            creditoCuentaRepository.save(cuentaCredito);
        }
    }

    private void recalcularTotalesVenta(Venta venta) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        BigDecimal impuesto = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (VentaDetalle detalle : venta.getDetalles()) {
            subtotal = subtotal.add(detalle.getSubtotal() != null ? detalle.getSubtotal() : BigDecimal.ZERO);
            descuento = descuento.add(detalle.getDescuentoMonto() != null ? detalle.getDescuentoMonto() : BigDecimal.ZERO);
            impuesto = impuesto.add(detalle.getImpuesto() != null ? detalle.getImpuesto() : BigDecimal.ZERO);
            total = total.add(detalle.getTotal() != null ? detalle.getTotal() : BigDecimal.ZERO);
        }
        venta.setSubtotal(subtotal);
        venta.setDescuento(descuento);
        venta.setImpuesto(impuesto);
        venta.setTotal(total);
    }

    private VentaHistorialItemResponse mapHistorialItem(Venta venta) {
        VentaHistorialItemResponse item = new VentaHistorialItemResponse();
        item.setId(venta.getId());
        item.setNumeroFactura(venta.getNumeroFactura());
        item.setFechaVenta(venta.getFechaVenta());
        if (venta.getCliente() != null) {
            String nombre = (venta.getCliente().getNombre() != null ? venta.getCliente().getNombre() : "")
                    + " " + (venta.getCliente().getApellido() != null ? venta.getCliente().getApellido() : "");
            item.setClienteNombre(nombre.trim());
            item.setClienteDni(venta.getCliente().getDni());
        }
        item.setTotal(venta.getTotal());
        item.setEstado(venta.getEstado() != null ? venta.getEstado().name() : null);
        item.setMetodoPago(venta.getMetodoPago());
        item.setMotivoCancelacion(venta.getMotivoCancelacion());
        int items = venta.getDetalles() != null ? venta.getDetalles().size() : 0;
        item.setCantidadItems(items);
        return item;
    }

    private VentaResponse mapVentaResponse(Venta venta) {
        return mapVentaResponseCompleto(venta);
    }

    private VentaResponse mapVentaResponseCompleto(Venta venta) {
        VentaResponse response = new VentaResponse();
        response.setId(venta.getId());
        response.setClienteId(venta.getCliente().getId());
        response.setClienteDni(venta.getCliente().getDni());
        if (venta.getCliente() != null) {
            String nombre = (venta.getCliente().getNombre() != null ? venta.getCliente().getNombre() : "")
                    + " " + (venta.getCliente().getApellido() != null ? venta.getCliente().getApellido() : "");
            response.setClienteNombre(nombre.trim());
        }
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
        response.setMotivoCancelacion(venta.getMotivoCancelacion());
        response.setFechaCancelacion(venta.getFechaCancelacion());
        if (venta.getDetalles() != null) {
            response.setDetalles(venta.getDetalles().stream().map(this::mapVentaDetalleResponse).collect(Collectors.toList()));
        }
        if (venta.getPagos() != null) {
            response.setPagos(venta.getPagos().stream().map(this::mapPagoVentaResponse).collect(Collectors.toList()));
        }
        List<VentaCambioArticulo> cambios = ventaCambioArticuloRepository.findByVentaIdOrderByCreatedAtDesc(venta.getId());
        response.setCambiosArticulo(cambios.stream().map(this::mapCambioArticuloResponse).collect(Collectors.toList()));
        return response;
    }

    private VentaCambioArticuloResponse mapCambioArticuloResponse(VentaCambioArticulo cambio) {
        VentaCambioArticuloResponse response = new VentaCambioArticuloResponse();
        response.setId(cambio.getId());
        response.setVentaId(cambio.getVenta().getId());
        response.setVentaDetalleId(cambio.getVentaDetalle() != null ? cambio.getVentaDetalle().getId() : null);
        response.setVarianteDevueltaId(cambio.getVarianteDevuelta() != null ? cambio.getVarianteDevuelta().getId() : null);
        response.setVarianteNuevaId(cambio.getVarianteNueva().getId());
        response.setCantidad(cambio.getCantidad());
        response.setMotivo(cambio.getMotivo());
        response.setPrecioAnterior(cambio.getPrecioAnterior());
        response.setPrecioNuevo(cambio.getPrecioNuevo());
        response.setDiferenciaPrecio(cambio.getDiferenciaPrecio());
        response.setObservaciones(cambio.getObservaciones());
        response.setCreatedAt(cambio.getCreatedAt());
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
        Articulo articulo = detalle.getArticulo();
        if (articulo != null) {
            response.setCodigoArticulo(articulo.getCodigo());
            response.setDescripcionArticulo(construirDescripcionArticulo(articulo, detalle.getVariante()));
        }
        VarianteArticulo variante = detalle.getVariante();
        if (variante != null) {
            if (variante.getTalle() != null) {
                response.setTalle(variante.getTalle().getNumero());
            }
            if (variante.getColor() != null) {
                response.setColor(variante.getColor().getNombre());
            }
        }
        return response;
    }

    private String construirDescripcionArticulo(Articulo articulo, VarianteArticulo variante) {
        StringBuilder sb = new StringBuilder();
        if (articulo.getCodigo() != null && !articulo.getCodigo().isBlank()) {
            sb.append(articulo.getCodigo());
        }
        if (articulo.getModelo() != null && !articulo.getModelo().isBlank()) {
            if (!sb.isEmpty()) sb.append(" · ");
            sb.append(articulo.getModelo());
        }
        if (variante != null) {
            if (variante.getTalle() != null && variante.getTalle().getNumero() != null) {
                if (!sb.isEmpty()) sb.append(" · ");
                sb.append("Talle ").append(variante.getTalle().getNumero());
            }
            if (variante.getColor() != null && variante.getColor().getNombre() != null) {
                if (!sb.isEmpty()) sb.append(" · ");
                sb.append(variante.getColor().getNombre());
            }
        }
        return sb.isEmpty() ? "Artículo #" + articulo.getId() : sb.toString();
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
