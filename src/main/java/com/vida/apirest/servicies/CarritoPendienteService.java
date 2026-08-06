package com.vida.apirest.servicies;

import com.vida.apirest.dto.carrito.*;
import com.vida.apirest.dto.venta.VentaCreateRequest;
import com.vida.apirest.dto.venta.VentaCreditoPersonalRequest;
import com.vida.apirest.dto.venta.VentaResponse;
import com.vida.apirest.model.almacen.Stock;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.model.persona.Empleado;
import com.vida.apirest.model.venta.CarritoPendiente;
import com.vida.apirest.model.venta.CarritoPendienteDetalle;
import com.vida.apirest.model.venta.Venta;
import com.vida.apirest.repositories.*;
import com.vida.apirest.security.SucursalScopeService;
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
public class CarritoPendienteService {

    private final CarritoPendienteRepository carritoRepository;
    private final ClienteRepository clienteRepository;
    private final SucursalRepository sucursalRepository;
    private final EmpleadoRepository empleadoRepository;
    private final VentaRepository ventaRepository;
    private final VentaService ventaService;
    private final PendienteDetalleResolver pendienteDetalleResolver;
    private final StockReservaService stockReservaService;
    private final SucursalScopeService sucursalScopeService;

    @Transactional
    public CarritoPendienteResponse crear(CarritoPendienteCreateRequest request) {
        validarRequestBase(request);
        sucursalScopeService.assertCanUse(request.getSucursalId());

        Cliente cliente = clienteRepository.findByDni(request.getClienteDni())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con DNI: " + request.getClienteDni()));

        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));

        Empleado empleado = null;
        if (request.getEmpleadoId() != null) {
            empleado = empleadoRepository.findById(request.getEmpleadoId())
                    .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
        }

        CarritoPendiente carrito = new CarritoPendiente();
        carrito.setNumeroComprobante("CP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        carrito.setCliente(cliente);
        carrito.setSucursal(sucursal);
        carrito.setEmpleado(empleado);
        carrito.setEstado(CarritoPendiente.EstadoCarrito.PENDIENTE);
        carrito.setObservaciones(request.getObservaciones());

        for (CarritoPendienteDetalleRequest detReq : request.getDetalles()) {
            PendienteDetalleResolver.DetalleResuelto resuelto = pendienteDetalleResolver.resolver(
                    detReq.getArticuloId(), detReq.getVarianteId(), detReq.getCantidad(),
                    sucursal.getId(), true);
            stockReservaService.reservar(resuelto.stock(), detReq.getCantidad(),
                    carrito.getNumeroComprobante(), StockReservaService.ModoReserva.CARRITO);

            CarritoPendienteDetalle det = new CarritoPendienteDetalle();
            det.setCarrito(carrito);
            det.setArticulo(resuelto.articulo());
            det.setVariante(resuelto.variante());
            det.setCantidad(detReq.getCantidad());
            det.setPrecioUnitario(resuelto.precio());
            carrito.getDetalles().add(det);
        }

        carrito = carritoRepository.save(carrito);
        return mapResponse(carritoRepository.findByIdWithDetalles(carrito.getId()).orElse(carrito));
    }

    @Transactional(readOnly = true)
    public List<CarritoPendienteResponse> listar(Long sucursalId, String estado) {
        Long scopedSucursalId = sucursalScopeService.enforceFilter(sucursalId);
        List<CarritoPendiente.EstadoCarrito> estados;
        if (estado == null || estado.isBlank() || "PENDIENTES".equalsIgnoreCase(estado)) {
            estados = List.of(CarritoPendiente.EstadoCarrito.PENDIENTE);
        } else {
            try {
                estados = List.of(CarritoPendiente.EstadoCarrito.valueOf(estado.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Estado inválido: " + estado);
            }
        }
        return carritoRepository.findAllWithDetalles(scopedSucursalId, estados).stream()
                .map(this::mapResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CarritoPendienteResponse obtener(Long id) {
        return mapResponse(buscarCarrito(id));
    }

    @Transactional
    public CarritoPendienteResponse cancelar(Long id) {
        CarritoPendiente carrito = buscarCarrito(id);
        if (carrito.getEstado() != CarritoPendiente.EstadoCarrito.PENDIENTE) {
            throw new RuntimeException("Solo se pueden cancelar carritos pendientes");
        }

        for (CarritoPendienteDetalle det : carrito.getDetalles()) {
            Stock stock = obtenerStock(det, carrito.getSucursal().getId());
            stockReservaService.liberar(stock, det.getCantidad(), carrito.getNumeroComprobante(),
                    StockReservaService.ModoReserva.CARRITO);
        }

        carrito.setEstado(CarritoPendiente.EstadoCarrito.CANCELADO);
        carrito.setFechaCierre(LocalDateTime.now());
        carrito = carritoRepository.save(carrito);
        return mapResponse(carritoRepository.findByIdWithDetalles(carrito.getId()).orElse(carrito));
    }

    @Transactional
    public CarritoPendienteResponse confirmar(Long id, ConfirmarCarritoPendienteRequest request) {
        CarritoPendiente carrito = buscarCarrito(id);
        validarCarritoPendiente(carrito);
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe incluir detalles para la venta");
        }
        if (request.getPagos() == null || request.getPagos().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos un pago");
        }

        consumirReservas(carrito);

        VentaCreateRequest ventaReq = new VentaCreateRequest();
        ventaReq.setSucursalId(carrito.getSucursal().getId());
        ventaReq.setClienteDni(carrito.getCliente().getDni());
        if (carrito.getEmpleado() != null) {
            ventaReq.setEmpleadoId(carrito.getEmpleado().getId());
        }
        ventaReq.setObservaciones(request.getObservaciones() != null
                ? request.getObservaciones()
                : "Venta por carrito pendiente " + carrito.getNumeroComprobante());
        ventaReq.setMetodoPago(request.getMetodoPago());
        ventaReq.setDetalles(request.getDetalles());
        ventaReq.setPagos(request.getPagos());
        ventaReq.setFacturaAfip(request.getFacturaAfip());

        VentaResponse ventaResp = ventaService.registrarVenta(ventaReq, false);
        vincularVenta(carrito, ventaResp.getId());

        carrito.setEstado(CarritoPendiente.EstadoCarrito.COBRADO);
        carrito.setFechaCierre(LocalDateTime.now());
        carrito = carritoRepository.save(carrito);
        return mapResponse(carritoRepository.findByIdWithDetalles(carrito.getId()).orElse(carrito));
    }

    @Transactional
    public CarritoPendienteResponse confirmarCredito(Long id, ConfirmarCarritoPendienteCreditoRequest request) {
        CarritoPendiente carrito = buscarCarrito(id);
        validarCarritoPendiente(carrito);
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe incluir detalles para la venta");
        }

        consumirReservas(carrito);

        VentaCreditoPersonalRequest creditoReq = new VentaCreditoPersonalRequest();
        creditoReq.setSucursalId(carrito.getSucursal().getId());
        creditoReq.setClienteDni(carrito.getCliente().getDni());
        if (carrito.getEmpleado() != null) {
            creditoReq.setEmpleadoId(carrito.getEmpleado().getId());
        }
        creditoReq.setObservaciones(request.getObservaciones() != null
                ? request.getObservaciones()
                : "Crédito por carrito pendiente " + carrito.getNumeroComprobante());
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
        vincularVenta(carrito, ventaResp.getId());

        carrito.setEstado(CarritoPendiente.EstadoCarrito.COBRADO);
        carrito.setFechaCierre(LocalDateTime.now());
        carrito = carritoRepository.save(carrito);
        return mapResponse(carritoRepository.findByIdWithDetalles(carrito.getId()).orElse(carrito));
    }

    private void validarCarritoPendiente(CarritoPendiente carrito) {
        if (carrito.getEstado() != CarritoPendiente.EstadoCarrito.PENDIENTE) {
            throw new RuntimeException("El carrito no admite cobro en su estado actual");
        }
    }

    private void consumirReservas(CarritoPendiente carrito) {
        for (CarritoPendienteDetalle det : carrito.getDetalles()) {
            Stock stock = obtenerStock(det, carrito.getSucursal().getId());
            stockReservaService.consumir(stock, det.getCantidad(), carrito.getNumeroComprobante(),
                    StockReservaService.ModoReserva.CARRITO);
        }
    }

    private void vincularVenta(CarritoPendiente carrito, Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta generada no encontrada"));
        carrito.setVenta(venta);
    }

    private CarritoPendiente buscarCarrito(Long id) {
        CarritoPendiente carrito = carritoRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Carrito pendiente no encontrado"));
        sucursalScopeService.assertCanAccess(carrito.getSucursal().getId());
        return carrito;
    }

    private void validarRequestBase(CarritoPendienteCreateRequest request) {
        if (request.getClienteDni() == null || request.getClienteDni().isBlank()) {
            throw new RuntimeException("DNI de cliente requerido");
        }
        if (request.getSucursalId() == null) {
            throw new RuntimeException("Sucursal requerida");
        }
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos un artículo en el carrito");
        }
    }

    private Stock obtenerStock(CarritoPendienteDetalle det, Long sucursalId) {
        Long varianteId = det.getVariante() != null ? det.getVariante().getId() : null;
        return pendienteDetalleResolver.obtenerStock(det.getArticulo().getId(), varianteId, sucursalId);
    }

    private CarritoPendienteResponse mapResponse(CarritoPendiente carrito) {
        CarritoPendienteResponse r = new CarritoPendienteResponse();
        r.setId(carrito.getId());
        r.setNumeroComprobante(carrito.getNumeroComprobante());
        r.setSucursalId(carrito.getSucursal().getId());
        r.setSucursalNombre(carrito.getSucursal().getNombre());
        r.setClienteId(carrito.getCliente().getId());
        r.setClienteDni(carrito.getCliente().getDni());
        r.setClienteNombre(carrito.getCliente().getNombre() + " " + carrito.getCliente().getApellido());
        if (carrito.getEmpleado() != null) {
            r.setEmpleadoNombre(carrito.getEmpleado().getNombre() + " " + carrito.getEmpleado().getApellido());
        }
        r.setEstado(carrito.getEstado().name());
        r.setFechaCreacion(carrito.getCreatedAt());
        r.setFechaCierre(carrito.getFechaCierre());
        r.setObservaciones(carrito.getObservaciones());
        if (carrito.getVenta() != null) {
            r.setVentaId(carrito.getVenta().getId());
            if (carrito.getVenta().getNumeroFactura() != null) {
                r.setNumeroFactura(carrito.getVenta().getNumeroFactura());
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        List<CarritoPendienteDetalleResponse> detalles = new ArrayList<>();
        for (CarritoPendienteDetalle det : carrito.getDetalles()) {
            CarritoPendienteDetalleResponse dr = new CarritoPendienteDetalleResponse();
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
            dr.setPrecioUnitario(det.getPrecioUnitario());
            BigDecimal sub = det.getPrecioUnitario() != null
                    ? det.getPrecioUnitario().multiply(BigDecimal.valueOf(det.getCantidad()))
                    : BigDecimal.ZERO;
            dr.setSubtotal(sub);
            total = total.add(sub);
            detalles.add(dr);
        }
        r.setDetalles(detalles);
        r.setTotal(total);
        return r;
    }
}
