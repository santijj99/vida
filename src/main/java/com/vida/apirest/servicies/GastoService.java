package com.vida.apirest.servicies;

import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.dto.finanzas.*;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.finanzas.CuentaFinanciera;
import com.vida.apirest.model.finanzas.Gasto;
import com.vida.apirest.model.finanzas.Gasto.EstadoGasto;
import com.vida.apirest.model.finanzas.GastoCategoria;
import com.vida.apirest.model.finanzas.GastoPago;
import com.vida.apirest.model.tesoreria.MovimientoFinanciero;
import com.vida.apirest.repositories.FinanzasCuentaFinancieraRepository;
import com.vida.apirest.repositories.GastoRepository;
import com.vida.apirest.repositories.MovimientoFinancieroRepository;
import com.vida.apirest.repositories.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GastoService {

    private static final int DEFAULT_PAGE_SIZE = 15;
    private static final int MAX_PAGE_SIZE = 100;

    private final GastoRepository gastoRepository;
    private final GastoCategoriaService gastoCategoriaService;
    private final SucursalRepository sucursalRepository;
    private final FinanzasCuentaFinancieraRepository cuentaRepository;
    private final MovimientoFinancieroRepository movimientoFinancieroRepository;

    @Transactional(readOnly = true)
    public PageResponse<GastoResponse> listar(
            Long sucursalId, String estado, Long categoriaId, String q, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE));
        EstadoGasto estadoEnum = parseEstado(estado);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Gasto> result = gastoRepository.searchPage(sucursalId, estadoEnum, categoriaId, q, pageable);
        return PageResponse.from(result.map(this::mapResumen));
    }

    @Transactional(readOnly = true)
    public GastoResponse findById(Long id) {
        return mapDetalle(buscarConRelaciones(id));
    }

    @Transactional
    public GastoResponse crear(GastoCreateRequest request) {
        validarCreate(request);
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada: " + request.getSucursalId()));
        GastoCategoria categoria = gastoCategoriaService.buscarEntidad(request.getCategoriaId());
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new RuntimeException("La categoría de gasto no está activa");
        }

        Gasto gasto = new Gasto();
        gasto.setSucursal(sucursal);
        gasto.setCategoria(categoria);
        gasto.setNumero(generarNumero());
        gasto.setDescripcion(request.getDescripcion().trim());
        gasto.setMonto(request.getMonto());
        gasto.setMonedaId(request.getMonedaId());
        gasto.setEstado(EstadoGasto.BORRADOR);
        gasto.setProveedor(blankToNull(request.getProveedor()));
        gasto.setNumeroComprobante(blankToNull(request.getNumeroComprobante()));
        gasto.setFechaComprobante(request.getFechaComprobante());
        gasto.setResponsable(blankToNull(request.getResponsable()));
        gasto.setObservaciones(blankToNull(request.getObservaciones()));

        gasto = gastoRepository.save(gasto);
        return mapDetalle(gastoRepository.findByIdWithRelations(gasto.getId()).orElse(gasto));
    }

    @Transactional
    public GastoResponse actualizar(Long id, GastoUpdateRequest request) {
        Gasto gasto = buscarConRelaciones(id);
        if (gasto.getEstado() == EstadoGasto.PAGADO || gasto.getEstado() == EstadoGasto.CANCELADO) {
            throw new RuntimeException("No se puede editar un gasto " + gasto.getEstado().name().toLowerCase());
        }
        if (!gasto.getPagos().isEmpty()) {
            throw new RuntimeException("No se puede editar un gasto que ya tiene pagos registrados");
        }
        if (request.getCategoriaId() != null) {
            GastoCategoria categoria = gastoCategoriaService.buscarEntidad(request.getCategoriaId());
            gasto.setCategoria(categoria);
        }
        if (request.getDescripcion() != null && !request.getDescripcion().isBlank()) {
            gasto.setDescripcion(request.getDescripcion().trim());
        }
        if (request.getMonto() != null) {
            if (request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("El monto debe ser mayor a cero");
            }
            gasto.setMonto(request.getMonto());
        }
        gasto.setMonedaId(request.getMonedaId());
        gasto.setProveedor(blankToNull(request.getProveedor()));
        gasto.setNumeroComprobante(blankToNull(request.getNumeroComprobante()));
        gasto.setFechaComprobante(request.getFechaComprobante());
        gasto.setResponsable(blankToNull(request.getResponsable()));
        gasto.setObservaciones(blankToNull(request.getObservaciones()));

        gasto = gastoRepository.save(gasto);
        return mapDetalle(gastoRepository.findByIdWithRelations(gasto.getId()).orElse(gasto));
    }

    @Transactional
    public GastoResponse aprobar(Long id) {
        Gasto gasto = buscarConRelaciones(id);
        if (gasto.getEstado() != EstadoGasto.BORRADOR) {
            throw new RuntimeException("Solo se pueden aprobar gastos en borrador");
        }
        gasto.setEstado(EstadoGasto.APROBADO);
        gasto = gastoRepository.save(gasto);
        return mapDetalle(gasto);
    }

    @Transactional
    public GastoResponse registrarPago(Long id, GastoPagoRequest request) {
        Gasto gasto = buscarConRelaciones(id);
        if (gasto.getEstado() == EstadoGasto.CANCELADO) {
            throw new RuntimeException("No se puede pagar un gasto cancelado");
        }
        if (gasto.getEstado() == EstadoGasto.PAGADO) {
            throw new RuntimeException("El gasto ya está pagado");
        }
        if (request.getCuentaId() == null) {
            throw new RuntimeException("Debe indicar la cuenta financiera de pago");
        }
        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto del pago debe ser mayor a cero");
        }

        BigDecimal totalPagado = sumarPagos(gasto);
        BigDecimal saldoPendiente = gasto.getMonto().subtract(totalPagado);
        if (request.getMonto().compareTo(saldoPendiente) > 0) {
            throw new RuntimeException("El pago supera el saldo pendiente (" + saldoPendiente + ")");
        }

        CuentaFinanciera cuenta = cuentaRepository.findById(request.getCuentaId())
                .orElseThrow(() -> new RuntimeException("Cuenta financiera no encontrada: " + request.getCuentaId()));
        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            throw new RuntimeException("La cuenta financiera no está activa");
        }
        if (!cuenta.getSucursal().getId().equals(gasto.getSucursal().getId())) {
            throw new RuntimeException("La cuenta debe pertenecer a la misma sucursal del gasto");
        }

        GastoPago pago = new GastoPago();
        pago.setGasto(gasto);
        pago.setCuenta(cuenta);
        pago.setMonto(request.getMonto());
        pago.setNumeroComprobante(blankToNull(request.getNumeroComprobante()));
        pago.setReferencia(blankToNull(request.getReferencia()));
        pago.setObservaciones(blankToNull(request.getObservaciones()));
        gasto.getPagos().add(pago);

        registrarEgresoCuenta(cuenta, pago, gasto.getNumero());

        totalPagado = totalPagado.add(request.getMonto());
        if (totalPagado.compareTo(gasto.getMonto()) >= 0) {
            gasto.setEstado(EstadoGasto.PAGADO);
        } else if (gasto.getEstado() == EstadoGasto.BORRADOR) {
            gasto.setEstado(EstadoGasto.APROBADO);
        }

        gasto = gastoRepository.save(gasto);
        return mapDetalle(gastoRepository.findByIdWithRelations(gasto.getId()).orElse(gasto));
    }

    @Transactional
    public GastoResponse cancelar(Long id) {
        Gasto gasto = buscarConRelaciones(id);
        if (gasto.getEstado() == EstadoGasto.PAGADO) {
            throw new RuntimeException("No se puede cancelar un gasto ya pagado");
        }
        if (!gasto.getPagos().isEmpty()) {
            throw new RuntimeException("No se puede cancelar un gasto con pagos registrados");
        }
        gasto.setEstado(EstadoGasto.CANCELADO);
        gasto = gastoRepository.save(gasto);
        return mapDetalle(gasto);
    }

    @Transactional(readOnly = true)
    public List<CuentaFinancieraResponse> listarCuentasPago(Long sucursalId) {
        if (sucursalId == null) {
            throw new RuntimeException("sucursalId es obligatorio");
        }
        return cuentaRepository.findBySucursalIdAndActivoTrueOrderByNombreAsc(sucursalId).stream()
                .map(this::mapCuenta)
                .collect(Collectors.toList());
    }

    private void registrarEgresoCuenta(CuentaFinanciera cuenta, GastoPago pago, String numeroGasto) {
        BigDecimal saldoAnterior = cuenta.getSaldoActual() != null ? cuenta.getSaldoActual() : BigDecimal.ZERO;
        BigDecimal saldoNuevo = saldoAnterior.subtract(pago.getMonto());
        cuenta.setSaldoActual(saldoNuevo);
        cuentaRepository.save(cuenta);

        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setCuenta(cuenta);
        movimiento.setNumero("MV-" + UUID.randomUUID().toString().replace("-", ""));
        movimiento.setTipo(MovimientoFinanciero.TipoMovimiento.EGRESO);
        movimiento.setMonto(pago.getMonto());
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoNuevo(saldoNuevo);
        movimiento.setDescripcion("Pago de gasto " + numeroGasto);
        movimiento.setReferencia(pago.getReferencia() != null ? pago.getReferencia() : pago.getNumeroComprobante());
        movimiento.setResponsable("sistema");
        movimientoFinancieroRepository.save(movimiento);
    }

    private Gasto buscarConRelaciones(Long id) {
        return gastoRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado: " + id));
    }

    private void validarCreate(GastoCreateRequest request) {
        if (request.getSucursalId() == null) {
            throw new RuntimeException("sucursalId es obligatorio");
        }
        if (request.getCategoriaId() == null) {
            throw new RuntimeException("categoriaId es obligatorio");
        }
        if (request.getDescripcion() == null || request.getDescripcion().isBlank()) {
            throw new RuntimeException("La descripción es obligatoria");
        }
        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto debe ser mayor a cero");
        }
    }

    private String generarNumero() {
        String numero;
        do {
            numero = "GS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (gastoRepository.existsByNumero(numero));
        return numero;
    }

    private EstadoGasto parseEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoGasto.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Estado de gasto inválido: " + estado);
        }
    }

    private BigDecimal sumarPagos(Gasto gasto) {
        return gasto.getPagos().stream()
                .map(GastoPago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private GastoResponse mapResumen(Gasto gasto) {
        GastoResponse r = mapBase(gasto);
        BigDecimal totalPagado = sumarPagos(gasto);
        r.setTotalPagado(totalPagado);
        r.setSaldoPendiente(gasto.getMonto().subtract(totalPagado));
        r.setPagos(List.of());
        return r;
    }

    private GastoResponse mapDetalle(Gasto gasto) {
        GastoResponse r = mapBase(gasto);
        BigDecimal totalPagado = sumarPagos(gasto);
        r.setTotalPagado(totalPagado);
        r.setSaldoPendiente(gasto.getMonto().subtract(totalPagado));
        List<GastoPagoResponse> pagos = new ArrayList<>();
        for (GastoPago pago : gasto.getPagos()) {
            pagos.add(mapPago(pago));
        }
        r.setPagos(pagos);
        return r;
    }

    private GastoResponse mapBase(Gasto gasto) {
        GastoResponse r = new GastoResponse();
        r.setId(gasto.getId());
        r.setNumero(gasto.getNumero());
        r.setSucursalId(gasto.getSucursal().getId());
        r.setSucursalNombre(gasto.getSucursal().getNombre());
        r.setCategoriaId(gasto.getCategoria().getId());
        r.setCategoriaNombre(gasto.getCategoria().getNombre());
        r.setDescripcion(gasto.getDescripcion());
        r.setMonto(gasto.getMonto());
        r.setMonedaId(gasto.getMonedaId());
        r.setEstado(gasto.getEstado() != null ? gasto.getEstado().name() : null);
        r.setProveedor(gasto.getProveedor());
        r.setNumeroComprobante(gasto.getNumeroComprobante());
        r.setFechaComprobante(gasto.getFechaComprobante());
        r.setResponsable(gasto.getResponsable());
        r.setObservaciones(gasto.getObservaciones());
        r.setCreatedAt(gasto.getCreatedAt());
        r.setUpdatedAt(gasto.getUpdatedAt());
        return r;
    }

    private GastoPagoResponse mapPago(GastoPago pago) {
        GastoPagoResponse r = new GastoPagoResponse();
        r.setId(pago.getId());
        CuentaFinanciera cuenta = pago.getCuenta();
        r.setCuentaId(cuenta.getId());
        r.setCuentaNombre(cuenta.getNombre());
        r.setCuentaNumero(cuenta.getNumero());
        r.setCuentaTipo(cuenta.getTipo() != null ? cuenta.getTipo().name() : null);
        r.setMonto(pago.getMonto());
        r.setNumeroComprobante(pago.getNumeroComprobante());
        r.setReferencia(pago.getReferencia());
        r.setObservaciones(pago.getObservaciones());
        r.setCreatedAt(pago.getCreatedAt());
        return r;
    }

    private CuentaFinancieraResponse mapCuenta(CuentaFinanciera cuenta) {
        CuentaFinancieraResponse r = new CuentaFinancieraResponse();
        r.setId(cuenta.getId());
        r.setSucursalId(cuenta.getSucursal().getId());
        r.setMonedaId(cuenta.getMoneda().getId());
        r.setNombre(cuenta.getNombre());
        r.setNumero(cuenta.getNumero());
        r.setTipo(cuenta.getTipo() != null ? cuenta.getTipo().name() : null);
        r.setBanco(cuenta.getBanco());
        r.setSaldoActual(cuenta.getSaldoActual());
        r.setActivo(cuenta.getActivo());
        return r;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
