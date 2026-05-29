package com.vida.apirest.servicies;

import com.vida.apirest.dto.prestamo.*;
import com.vida.apirest.dto.venta.VentaCreateRequest;
import com.vida.apirest.dto.venta.VentaCreditoPersonalRequest;
import com.vida.apirest.dto.venta.VentaDetalleRequest;
import com.vida.apirest.dto.venta.VentaResponse;
import com.vida.apirest.model.almacen.Stock;
import com.vida.apirest.model.almacen.StockMovimiento;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.model.articulo.VarianteArticulo;
import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.model.persona.Empleado;
import com.vida.apirest.model.venta.PrestamoCondicional;
import com.vida.apirest.model.venta.PrestamoCondicionalDetalle;
import com.vida.apirest.model.venta.Venta;
import com.vida.apirest.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrestamoCondicionalService {

    private final PrestamoCondicionalRepository prestamoRepository;
    private final ClienteRepository clienteRepository;
    private final ArticuloRepository articuloRepository;
    private final VarianteArticuloRepository varianteArticuloRepository;
    private final StockRepository stockRepository;
    private final StockMovimientoRepository stockMovimientoRepository;
    private final SucursalRepository sucursalRepository;
    private final EmpleadoRepository empleadoRepository;
    private final VentaRepository ventaRepository;
    private final VentaService ventaService;

    @Transactional
    public PrestamoCondicionalResponse crear(PrestamoCondicionalCreateRequest request) {
        validarRequestBase(request);

        Cliente cliente = clienteRepository.findByDni(request.getClienteDni())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con DNI: " + request.getClienteDni()));

        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));

        Empleado empleado = null;
        if (request.getEmpleadoId() != null) {
            empleado = empleadoRepository.findById(request.getEmpleadoId())
                    .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
        }

        PrestamoCondicional prestamo = new PrestamoCondicional();
        prestamo.setNumeroComprobante("PC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        prestamo.setCliente(cliente);
        prestamo.setSucursal(sucursal);
        prestamo.setEmpleado(empleado);
        prestamo.setEstado(PrestamoCondicional.EstadoPrestamo.ABIERTO);
        prestamo.setFechaEntrega(LocalDateTime.now());
        prestamo.setFechaLimite(request.getFechaLimite());
        prestamo.setObservaciones(request.getObservaciones());

        for (var detReq : request.getDetalles()) {
            var resuelto = resolverDetalle(detReq, sucursal.getId());
            reservarStock(resuelto.stock(), detReq.getCantidad(), prestamo.getNumeroComprobante());

            PrestamoCondicionalDetalle det = new PrestamoCondicionalDetalle();
            det.setPrestamo(prestamo);
            det.setArticulo(resuelto.articulo());
            det.setVariante(resuelto.variante());
            det.setCantidad(detReq.getCantidad());
            det.setPrecioUnitarioReferencia(resuelto.precio());
            det.setEstado(PrestamoCondicionalDetalle.EstadoDetalle.PENDIENTE);
            prestamo.getDetalles().add(det);
        }

        prestamo = prestamoRepository.save(prestamo);
        return mapResponse(prestamo);
    }

    @Transactional(readOnly = true)
    public List<PrestamoCondicionalResponse> listar(Long sucursalId, String estado) {
        List<PrestamoCondicional.EstadoPrestamo> estados;
        if (estado == null || estado.isBlank() || "ACTIVOS".equalsIgnoreCase(estado)) {
            estados = List.of(
                    PrestamoCondicional.EstadoPrestamo.ABIERTO,
                    PrestamoCondicional.EstadoPrestamo.PARCIAL);
        } else {
            try {
                estados = List.of(PrestamoCondicional.EstadoPrestamo.valueOf(estado.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Estado inválido: " + estado);
            }
        }
        return prestamoRepository.findAllWithDetalles(sucursalId, estados)
                .stream()
                .map(this::mapResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PrestamoCondicionalResponse obtener(Long id) {
        return mapResponse(buscarPrestamo(id));
    }

    @Transactional
    public PrestamoCondicionalResponse devolver(Long id) {
        PrestamoCondicional prestamo = buscarPrestamo(id);
        validarPrestamoEditable(prestamo);
        List<Long> idsPendientes = prestamo.getDetalles().stream()
                .filter(d -> d.getEstado() == PrestamoCondicionalDetalle.EstadoDetalle.PENDIENTE)
                .map(PrestamoCondicionalDetalle::getId)
                .collect(Collectors.toList());
        if (idsPendientes.isEmpty()) {
            throw new RuntimeException("No hay artículos pendientes para devolver");
        }
        return devolverDetalles(id, idsPendientes);
    }

    @Transactional
    public PrestamoCondicionalResponse devolverDetalles(Long id, DevolverPrestamoDetallesRequest request) {
        if (request.getDetalleIds() == null || request.getDetalleIds().isEmpty()) {
            throw new RuntimeException("Debe indicar al menos un detalle a devolver");
        }
        return devolverDetalles(id, request.getDetalleIds());
    }

    @Transactional
    public PrestamoCondicionalResponse devolverDetalles(Long id, List<Long> detalleIds) {
        PrestamoCondicional prestamo = buscarPrestamo(id);
        validarPrestamoEditable(prestamo);

        List<PrestamoCondicionalDetalle> aDevolver = resolverDetallesPendientes(prestamo, detalleIds);
        for (PrestamoCondicionalDetalle det : aDevolver) {
            Stock stock = obtenerStock(det, prestamo.getSucursal().getId());
            liberarReserva(stock, det.getCantidad(), prestamo.getNumeroComprobante());
            det.setEstado(PrestamoCondicionalDetalle.EstadoDetalle.DEVUELTO);
        }

        actualizarEstadoCabecera(prestamo);
        prestamo = prestamoRepository.save(prestamo);
        return mapResponse(prestamoRepository.findByIdWithDetalles(prestamo.getId()).orElse(prestamo));
    }

    @Transactional
    public PrestamoCondicionalResponse confirmarCompra(Long id, ConfirmarPrestamoRequest request) {
        PrestamoCondicional prestamo = buscarPrestamo(id);
        validarPrestamoEditable(prestamo);
        if (request.getPrestamoDetalleIds() == null || request.getPrestamoDetalleIds().isEmpty()) {
            throw new RuntimeException("Debe indicar los artículos a confirmar (prestamoDetalleIds)");
        }
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe incluir detalles para la venta");
        }
        if (request.getPagos() == null || request.getPagos().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos un pago para confirmar la compra");
        }

        List<PrestamoCondicionalDetalle> aConfirmar =
                resolverDetallesPendientes(prestamo, request.getPrestamoDetalleIds());
        consumirReservasYConfirmarLineas(prestamo, aConfirmar);

        VentaCreateRequest ventaReq = new VentaCreateRequest();
        ventaReq.setSucursalId(prestamo.getSucursal().getId());
        ventaReq.setClienteDni(prestamo.getCliente().getDni());
        if (prestamo.getEmpleado() != null) {
            ventaReq.setEmpleadoId(prestamo.getEmpleado().getId());
        }
        ventaReq.setObservaciones(request.getObservaciones() != null
                ? request.getObservaciones()
                : "Venta por confirmación de préstamo " + prestamo.getNumeroComprobante());
        ventaReq.setMetodoPago(request.getMetodoPago());
        ventaReq.setDetalles(request.getDetalles());
        ventaReq.setPagos(request.getPagos());

        VentaResponse ventaResp = ventaService.registrarVenta(ventaReq, false);
        vincularVentaSiCorresponde(prestamo, ventaResp.getId());

        for (PrestamoCondicionalDetalle det : aConfirmar) {
            det.setEstado(PrestamoCondicionalDetalle.EstadoDetalle.CONFIRMADO);
        }
        actualizarEstadoCabecera(prestamo);
        prestamo = prestamoRepository.save(prestamo);
        return mapResponse(prestamoRepository.findByIdWithDetalles(prestamo.getId()).orElse(prestamo));
    }

    @Transactional
    public PrestamoCondicionalResponse confirmarCreditoPersonal(Long id, ConfirmarPrestamoCreditoRequest request) {
        PrestamoCondicional prestamo = buscarPrestamo(id);
        validarPrestamoEditable(prestamo);
        if (request.getPrestamoDetalleIds() == null || request.getPrestamoDetalleIds().isEmpty()) {
            throw new RuntimeException("Debe indicar los artículos a confirmar (prestamoDetalleIds)");
        }
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe incluir detalles para la venta");
        }

        List<PrestamoCondicionalDetalle> aConfirmar =
                resolverDetallesPendientes(prestamo, request.getPrestamoDetalleIds());
        consumirReservasYConfirmarLineas(prestamo, aConfirmar);

        VentaCreditoPersonalRequest creditoReq = new VentaCreditoPersonalRequest();
        creditoReq.setSucursalId(prestamo.getSucursal().getId());
        creditoReq.setClienteDni(prestamo.getCliente().getDni());
        if (prestamo.getEmpleado() != null) {
            creditoReq.setEmpleadoId(prestamo.getEmpleado().getId());
        }
        creditoReq.setObservaciones(request.getObservaciones() != null
                ? request.getObservaciones()
                : "Crédito personal por préstamo " + prestamo.getNumeroComprobante());
        creditoReq.setDetalles(request.getDetalles());
        creditoReq.setCreditoPlazoMeses(request.getCreditoPlazoMeses());
        creditoReq.setCreditoTasaInteres(request.getCreditoTasaInteres());
        creditoReq.setCreditoDescripcion(request.getCreditoDescripcion());
        creditoReq.setMontoAnticipo(request.getMontoAnticipo());
        creditoReq.setMetodoPagoAnticipo(request.getMetodoPagoAnticipo());
        creditoReq.setCuentaIdAnticipo(request.getCuentaIdAnticipo());
        creditoReq.setModoDistribucion(request.getModoDistribucion());
        creditoReq.setFechaPrimerVencimiento(request.getFechaPrimerVencimiento());

        VentaResponse ventaResp = ventaService.registrarVentaCreditoPersonal(creditoReq, false);
        vincularVentaSiCorresponde(prestamo, ventaResp.getId());

        for (PrestamoCondicionalDetalle det : aConfirmar) {
            det.setEstado(PrestamoCondicionalDetalle.EstadoDetalle.CONFIRMADO);
        }
        actualizarEstadoCabecera(prestamo);
        prestamo = prestamoRepository.save(prestamo);
        return mapResponse(prestamoRepository.findByIdWithDetalles(prestamo.getId()).orElse(prestamo));
    }

    private void validarPrestamoEditable(PrestamoCondicional prestamo) {
        if (prestamo.getEstado() != PrestamoCondicional.EstadoPrestamo.ABIERTO
                && prestamo.getEstado() != PrestamoCondicional.EstadoPrestamo.PARCIAL) {
            throw new RuntimeException("El préstamo no admite más operaciones en su estado actual");
        }
    }

    private List<PrestamoCondicionalDetalle> resolverDetallesPendientes(
            PrestamoCondicional prestamo, List<Long> detalleIds) {
        List<PrestamoCondicionalDetalle> encontrados = prestamo.getDetalles().stream()
                .filter(d -> detalleIds.contains(d.getId()))
                .collect(Collectors.toList());
        if (encontrados.size() != detalleIds.size()) {
            throw new RuntimeException("Uno o más detalles no pertenecen al préstamo");
        }
        boolean algunoNoPendiente = encontrados.stream()
                .anyMatch(d -> d.getEstado() != PrestamoCondicionalDetalle.EstadoDetalle.PENDIENTE);
        if (algunoNoPendiente) {
            throw new RuntimeException("Solo se pueden operar líneas en estado PENDIENTE");
        }
        return encontrados;
    }

    private void consumirReservasYConfirmarLineas(
            PrestamoCondicional prestamo, List<PrestamoCondicionalDetalle> lineas) {
        for (PrestamoCondicionalDetalle det : lineas) {
            Stock stock = obtenerStock(det, prestamo.getSucursal().getId());
            consumirReserva(stock, det.getCantidad(), prestamo.getNumeroComprobante());
        }
    }

    private void vincularVentaSiCorresponde(PrestamoCondicional prestamo, Long ventaId) {
        if (prestamo.getVenta() == null) {
            Venta venta = ventaRepository.findById(ventaId)
                    .orElseThrow(() -> new RuntimeException("Venta generada no encontrada"));
            prestamo.setVenta(venta);
        }
    }

    private void actualizarEstadoCabecera(PrestamoCondicional prestamo) {
        long pendientes = prestamo.getDetalles().stream()
                .filter(d -> d.getEstado() == PrestamoCondicionalDetalle.EstadoDetalle.PENDIENTE)
                .count();
        if (pendientes > 0) {
            boolean algunoResuelto = prestamo.getDetalles().stream()
                    .anyMatch(d -> d.getEstado() != PrestamoCondicionalDetalle.EstadoDetalle.PENDIENTE);
            prestamo.setEstado(algunoResuelto
                    ? PrestamoCondicional.EstadoPrestamo.PARCIAL
                    : PrestamoCondicional.EstadoPrestamo.ABIERTO);
            prestamo.setFechaCierre(null);
            return;
        }

        prestamo.setFechaCierre(LocalDateTime.now());
        boolean allDevuelto = prestamo.getDetalles().stream()
                .allMatch(d -> d.getEstado() == PrestamoCondicionalDetalle.EstadoDetalle.DEVUELTO);
        boolean allConfirmado = prestamo.getDetalles().stream()
                .allMatch(d -> d.getEstado() == PrestamoCondicionalDetalle.EstadoDetalle.CONFIRMADO);
        if (allDevuelto) {
            prestamo.setEstado(PrestamoCondicional.EstadoPrestamo.DEVUELTO);
        } else if (allConfirmado) {
            prestamo.setEstado(PrestamoCondicional.EstadoPrestamo.CONFIRMADO);
        } else {
            prestamo.setEstado(PrestamoCondicional.EstadoPrestamo.PARCIAL);
        }
    }

    private PrestamoCondicional buscarPrestamo(Long id) {
        return prestamoRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Préstamo condicional no encontrado"));
    }

    private void validarRequestBase(PrestamoCondicionalCreateRequest request) {
        if (request.getClienteDni() == null || request.getClienteDni().isBlank()) {
            throw new RuntimeException("DNI de cliente requerido");
        }
        if (request.getSucursalId() == null) {
            throw new RuntimeException("Sucursal requerida");
        }
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos un artículo en el préstamo");
        }
    }

    private record DetalleResuelto(Articulo articulo, VarianteArticulo variante, Stock stock, BigDecimal precio) {}

    private DetalleResuelto resolverDetalle(PrestamoCondicionalDetalleRequest detReq, Long sucursalId) {
        if (detReq.getCantidad() == null || detReq.getCantidad() <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a cero");
        }

        VarianteArticulo variante = null;
        Articulo articulo;

        if (detReq.getVarianteId() != null) {
            variante = varianteArticuloRepository.findById(detReq.getVarianteId())
                    .orElseThrow(() -> new RuntimeException("Variante no encontrada"));
            articulo = articuloRepository.findById(variante.getArticuloId())
                    .orElseThrow(() -> new RuntimeException("Artículo no encontrado"));
        } else {
            if (detReq.getArticuloId() == null) {
                throw new RuntimeException("Cada detalle requiere articuloId o varianteId");
            }
            articulo = articuloRepository.findById(detReq.getArticuloId())
                    .orElseThrow(() -> new RuntimeException("Artículo no encontrado"));
        }

        BigDecimal precio = variante != null
                ? obtenerPrecioVariante(variante)
                : BigDecimal.ZERO;

        Stock stock = variante != null
                ? findStockByVariante(variante.getId(), sucursalId)
                : findStock(articulo.getId(), null, sucursalId);

        return new DetalleResuelto(articulo, variante, stock, precio);
    }

    private Stock obtenerStock(PrestamoCondicionalDetalle det, Long sucursalId) {
        return det.getVariante() != null
                ? findStockByVariante(det.getVariante().getId(), sucursalId)
                : findStock(det.getArticulo().getId(), null, sucursalId);
    }

    private void reservarStock(Stock stock, Integer cantidad, String referencia) {
        int disponible = stock.getCantidadDisponible() != null ? stock.getCantidadDisponible() : 0;
        if (disponible < cantidad) {
            throw new RuntimeException("Stock insuficiente para el artículo");
        }

        int actual = stock.getCantidadActual() != null ? stock.getCantidadActual() : 0;
        int reservada = stock.getCantidadReservada() != null ? stock.getCantidadReservada() : 0;

        stock.setCantidadDisponible(disponible - cantidad);
        stock.setCantidadActual(Math.max(0, actual - cantidad));
        stock.setCantidadReservada(reservada + cantidad);
        stockRepository.save(stock);

        registrarMovimiento(stock, StockMovimiento.TipoMovimiento.RESERVA_PRESTAMO,
                cantidad, disponible, disponible - cantidad, referencia,
                "Reserva por préstamo condicional");
    }

    private void liberarReserva(Stock stock, Integer cantidad, String referencia) {
        int reservada = stock.getCantidadReservada() != null ? stock.getCantidadReservada() : 0;
        if (reservada < cantidad) {
            throw new RuntimeException("Cantidad reservada insuficiente para liberar");
        }

        int disponible = stock.getCantidadDisponible() != null ? stock.getCantidadDisponible() : 0;
        int actual = stock.getCantidadActual() != null ? stock.getCantidadActual() : 0;

        stock.setCantidadReservada(reservada - cantidad);
        stock.setCantidadDisponible(disponible + cantidad);
        stock.setCantidadActual(actual + cantidad);
        stockRepository.save(stock);

        registrarMovimiento(stock, StockMovimiento.TipoMovimiento.LIBERACION_RESERVA_PRESTAMO,
                cantidad, disponible, disponible + cantidad, referencia,
                "Devolución de préstamo condicional");
    }

    private void consumirReserva(Stock stock, Integer cantidad, String referencia) {
        int reservada = stock.getCantidadReservada() != null ? stock.getCantidadReservada() : 0;
        if (reservada < cantidad) {
            throw new RuntimeException("Cantidad reservada insuficiente");
        }

        int disponible = stock.getCantidadDisponible() != null ? stock.getCantidadDisponible() : 0;
        stock.setCantidadReservada(reservada - cantidad);
        stockRepository.save(stock);

        registrarMovimiento(stock, StockMovimiento.TipoMovimiento.SALIDA_VENTA,
                cantidad, disponible, disponible, referencia,
                "Confirmación de compra por préstamo condicional");
    }

    private void registrarMovimiento(Stock stock, StockMovimiento.TipoMovimiento tipo,
                                     int cantidad, int saldoAnterior, int saldoNuevo,
                                     String referencia, String descripcion) {
        StockMovimiento mov = new StockMovimiento();
        mov.setStock(stock);
        mov.setTipo(tipo);
        mov.setCantidad(cantidad);
        mov.setSaldoAnterior(saldoAnterior);
        mov.setSaldoNuevo(saldoNuevo);
        mov.setReferencia(referencia);
        mov.setDescripcion(descripcion);
        mov.setUsuario("sistema");
        stockMovimientoRepository.save(mov);
    }

    private Stock findStock(Long articuloId, Long varianteId, Long sucursalId) {
        return varianteId != null
                ? stockRepository.findByArticuloIdAndVarianteIdAndSucursalId(articuloId, varianteId, sucursalId)
                .orElseThrow(() -> new RuntimeException("Stock no encontrado"))
                : stockRepository.findByArticuloIdAndSucursalId(articuloId, sucursalId)
                .orElseThrow(() -> new RuntimeException("Stock no encontrado"));
    }

    private Stock findStockByVariante(Long varianteId, Long sucursalId) {
        return stockRepository.findByVarianteIdAndSucursalId(varianteId, sucursalId)
                .orElseThrow(() -> new RuntimeException("Stock no encontrado para la variante"));
    }

    private BigDecimal obtenerPrecioVariante(VarianteArticulo variante) {
        if (variante.getHistorialPrecios() != null && !variante.getHistorialPrecios().isEmpty()) {
            return variante.getHistorialPrecios().stream()
                    .max((a, b) -> a.getFecha().compareTo(b.getFecha()))
                    .map(h -> h.getPrecioNuevo())
                    .orElse(BigDecimal.ZERO);
        }
        if (variante.getListaPrecio() != null && variante.getListaPrecio().getPrecio() != null) {
            return variante.getListaPrecio().getPrecio();
        }
        return BigDecimal.ZERO;
    }

    private PrestamoCondicionalResponse mapResponse(PrestamoCondicional prestamo) {
        PrestamoCondicionalResponse r = new PrestamoCondicionalResponse();
        r.setId(prestamo.getId());
        r.setNumeroComprobante(prestamo.getNumeroComprobante());
        r.setSucursalId(prestamo.getSucursal().getId());
        r.setSucursalNombre(prestamo.getSucursal().getNombre());
        r.setClienteId(prestamo.getCliente().getId());
        r.setClienteDni(prestamo.getCliente().getDni());
        r.setClienteNombre(prestamo.getCliente().getNombre() + " " + prestamo.getCliente().getApellido());
        r.setEstado(prestamo.getEstado().name());
        r.setFechaEntrega(prestamo.getFechaEntrega());
        r.setFechaLimite(prestamo.getFechaLimite());
        r.setFechaCierre(prestamo.getFechaCierre());
        r.setObservaciones(prestamo.getObservaciones());
        if (prestamo.getVenta() != null) {
            r.setVentaId(prestamo.getVenta().getId());
            if (prestamo.getVenta().getNumeroFactura() != null) {
                r.setNumeroFactura(prestamo.getVenta().getNumeroFactura());
            }
        }

        BigDecimal totalRef = BigDecimal.ZERO;
        BigDecimal totalPendiente = BigDecimal.ZERO;
        List<PrestamoCondicionalDetalleResponse> detalles = new ArrayList<>();
        for (PrestamoCondicionalDetalle det : prestamo.getDetalles()) {
            PrestamoCondicionalDetalleResponse dr = new PrestamoCondicionalDetalleResponse();
            dr.setId(det.getId());
            dr.setArticuloId(det.getArticulo().getId());
            if (det.getVariante() != null) {
                dr.setVarianteId(det.getVariante().getId());
                dr.setCodigo(det.getVariante().getCodigoBarras() != null
                        ? det.getVariante().getCodigoBarras()
                        : String.valueOf(det.getArticulo().getId()));
                if (det.getVariante().getTalle() != null) {
                    dr.setTalle(det.getVariante().getTalle().getNumero());
                }
                if (det.getVariante().getColor() != null) {
                    dr.setColor(det.getVariante().getColor().getNombre());
                }
            } else {
                dr.setCodigo(String.valueOf(det.getArticulo().getId()));
            }
            String marca = det.getArticulo().getMarca() != null ? det.getArticulo().getMarca().getNombre() : "";
            String modelo = det.getArticulo().getModelo() != null ? det.getArticulo().getModelo() : "";
            dr.setDescripcion((marca + " " + modelo).trim());
            dr.setCantidad(det.getCantidad());
            dr.setPrecioUnitarioReferencia(det.getPrecioUnitarioReferencia());
            BigDecimal sub = det.getPrecioUnitarioReferencia() != null
                    ? det.getPrecioUnitarioReferencia().multiply(BigDecimal.valueOf(det.getCantidad()))
                    : BigDecimal.ZERO;
            dr.setSubtotalReferencia(sub);
            dr.setEstado(det.getEstado().name());
            totalRef = totalRef.add(sub);
            if (det.getEstado() == PrestamoCondicionalDetalle.EstadoDetalle.PENDIENTE) {
                totalPendiente = totalPendiente.add(sub);
            }
            detalles.add(dr);
        }
        r.setDetalles(detalles);
        r.setTotalReferencia(totalRef);
        r.setTotalPendiente(totalPendiente);
        return r;
    }
}
