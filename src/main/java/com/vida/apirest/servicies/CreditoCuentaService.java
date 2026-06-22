package com.vida.apirest.servicies;

import com.vida.apirest.dto.credito.*;
import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.dto.dashboard.DashboardCuotaPorEstadoResponse;
import com.vida.apirest.dto.dashboard.DashboardCuentaCreditoItemResponse;
import com.vida.apirest.dto.dashboard.DashboardCreditosResumenResponse;
import com.vida.apirest.model.credito.Credito;
import com.vida.apirest.model.credito.Cuota;
import com.vida.apirest.model.credito.Cuenta;
import com.vida.apirest.model.credito.PagoCuota;
import com.vida.apirest.model.finanzas.CuentaFinanciera;
import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.model.tesoreria.MovimientoFinanciero;
import com.vida.apirest.repositories.CreditoResumenPorCliente;
import com.vida.apirest.repositories.CreditoRepository;
import com.vida.apirest.repositories.CuotaRepository;
import com.vida.apirest.repositories.CuentaRepository;
import com.vida.apirest.repositories.DashboardQueryRepository;
import com.vida.apirest.repositories.FinanzasCuentaFinancieraRepository;
import com.vida.apirest.repositories.MovimientoFinancieroRepository;
import com.vida.apirest.repositories.PagoCuotaRepository;
import com.vida.apirest.security.SucursalScopeService;
import com.vida.apirest.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditoCuentaService {

    private final CuentaRepository creditoCuentaRepository;
    private final CreditoRepository creditoRepository;
    private final CuotaRepository cuotaRepository;
    private final FinanzasCuentaFinancieraRepository cuentaFinancieraRepository;
    private final MovimientoFinancieroRepository movimientoFinancieroRepository;
    private final PagoCuotaRepository pagoCuotaRepository;
    private final DashboardQueryRepository dashboardQueryRepository;
    private final CajaMovimientoService cajaMovimientoService;
    private final SucursalScopeService sucursalScopeService;

    @Transactional(readOnly = true)
    public List<CuentaCreditoListResponse> listarCuentas(Long sucursalId) {
        Long scopedSucursalId = sucursalScopeService.enforceFilter(sucursalId);
        List<Cuenta> cuentas = scopedSucursalId != null
                ? creditoCuentaRepository.findActivasBySucursalWithCliente(scopedSucursalId)
                : creditoCuentaRepository.findAllActivasWithCliente();
        Map<Long, CreditoResumenPorCliente> resumenMap = loadResumenMap(
                cuentas.stream().map(x -> x.getCliente().getId()).distinct().toList());
        Set<Long> clientesVencidos = loadClientesConVencidos(
                cuentas.stream().map(x -> x.getCliente().getId()).distinct().toList());
        return cuentas.stream()
                .map(c -> enrichWithResumen(mapCuentaList(c), resumenMap, clientesVencidos))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<CuentaCreditoListResponse> listarCuentasPage(
            Long sucursalId, String q, String estadoCredito, int page, int size
    ) {
        PaginationUtils.PageParams params = PaginationUtils.normalize(page, size);
        String query = PaginationUtils.normalizeQuery(q);
        String estadoFilter = normalizeEstadoCreditoFilter(estadoCredito);
        Long scopedSucursalId = sucursalScopeService.enforceFilter(sucursalId);
        Pageable pageable = PageRequest.of(
                params.page(),
                params.size(),
                Sort.by(Sort.Direction.DESC, "saldoActual")
        );
        Page<Cuenta> cuentaPage = creditoCuentaRepository.searchPage(
                scopedSucursalId, query, estadoFilter, pageable);
        List<Long> clienteIds = cuentaPage.getContent().stream()
                .map(c -> c.getCliente().getId()).distinct().toList();
        Map<Long, CreditoResumenPorCliente> resumenMap = loadResumenMap(clienteIds);
        Set<Long> clientesVencidos = loadClientesConVencidos(clienteIds);
        return PageResponse.from(cuentaPage.map(c -> enrichWithResumen(
                mapCuentaList(c),
                resumenMap,
                clientesVencidos
        )));
    }

    private String normalizeEstadoCreditoFilter(String estadoCredito) {
        if (estadoCredito == null || estadoCredito.isBlank()) {
            return "TODOS";
        }
        return estadoCredito.trim().toUpperCase();
    }

    private Set<Long> loadClientesConVencidos(List<Long> clienteIds) {
        if (clienteIds == null || clienteIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(creditoRepository.findClienteIdsConCreditosVencidos(clienteIds));
    }

    private Map<Long, CreditoResumenPorCliente> loadResumenMap(List<Long> clienteIds) {
        if (clienteIds == null || clienteIds.isEmpty()) {
            return Map.of();
        }
        return creditoRepository.resumenPorClientes(clienteIds).stream()
                .collect(Collectors.toMap(CreditoResumenPorCliente::getClienteId, r -> r));
    }

    private CuentaCreditoListResponse enrichWithResumen(
            CuentaCreditoListResponse dto,
            Map<Long, CreditoResumenPorCliente> resumenMap,
            Set<Long> clientesConVencidos
    ) {
        CreditoResumenPorCliente resumen = resumenMap.get(dto.getClienteId());
        if (resumen != null) {
            dto.setCantidadCreditos(resumen.getCantidadCreditos() != null
                    ? resumen.getCantidadCreditos().intValue() : 0);
            dto.setTotalCreditosSacados(resumen.getTotalCreditosSacados());
            dto.setTotalPagado(resumen.getTotalPagado());
        } else {
            dto.setCantidadCreditos(0);
            dto.setTotalCreditosSacados(BigDecimal.ZERO);
            dto.setTotalPagado(BigDecimal.ZERO);
        }
        dto.setTieneCreditosVencidos(clientesConVencidos.contains(dto.getClienteId()));
        return dto;
    }

    private void aplicarResumenCreditos(
            ClienteCreditosResponse response,
            List<Credito> creditos
    ) {
        BigDecimal totalCreditos = BigDecimal.ZERO;
        BigDecimal totalPagado = BigDecimal.ZERO;
        int cantidad = 0;
        for (Credito c : creditos) {
            if (c.getEstado() == Credito.EstadoCredito.CANCELADO) {
                continue;
            }
            cantidad++;
            BigDecimal importe = c.getImporte() != null ? c.getImporte() : BigDecimal.ZERO;
            BigDecimal saldo = c.getSaldo() != null ? c.getSaldo() : BigDecimal.ZERO;
            totalCreditos = totalCreditos.add(importe);
            totalPagado = totalPagado.add(importe.subtract(saldo).max(BigDecimal.ZERO));
        }
        response.setCantidadCreditos(cantidad);
        response.setTotalCreditosSacados(totalCreditos);
        response.setTotalPagado(totalPagado);
    }

    @Transactional(readOnly = true)
    public ClienteCreditosResponse obtenerCreditosPorCuenta(Long cuentaId) {
        Cuenta cuenta = creditoCuentaRepository.findByIdWithCliente(cuentaId)
                .orElseThrow(() -> new RuntimeException("Cuenta de crédito no encontrada"));
        sucursalScopeService.assertCanAccess(cuenta.getSucursal().getId());
        return construirClienteCreditos(cuenta);
    }

    @Transactional(readOnly = true)
    public ClienteCreditosResponse obtenerCreditosPorCliente(Long clienteId) {
        Cuenta cuenta = creditoCuentaRepository.findActivaByClienteId(clienteId)
                .orElseThrow(() -> new RuntimeException("Cuenta de crédito no encontrada para el cliente"));
        sucursalScopeService.assertCanAccess(cuenta.getSucursal().getId());
        return construirClienteCreditos(cuenta);
    }

    @Transactional
    public PagoCuotasResponse pagarCuotas(PagoCuotasRequest request) {
        validarSolicitudPagoCuotas(request);
        List<Cuota> cuotas = cargarCuotasValidadas(request.getCuotaIds());
        sucursalScopeService.assertCanAccess(cuotas.get(0).getCredito().getSucursal().getId());
        BigDecimal totalCuotas = calcularTotalPendiente(cuotas);

        if (request.getMontoEntregado().compareTo(totalCuotas) < 0) {
            throw new RuntimeException("El monto entregado es menor al total de las cuotas seleccionadas");
        }

        Long clienteId = cuotas.get(0).getCredito().getCliente().getId();
        Map<Long, BigDecimal> pagadoPorCreditoId = aplicarPagoACuotas(cuotas, request);
        actualizarSaldosCreditos(pagadoPorCreditoId);
        descontarSaldoCuentaCliente(clienteId, cuotas.get(0).getCredito().getSucursal().getId(), totalCuotas);

        return construirRespuestaPagoCuotas(request, totalCuotas, cuotas);
    }

    private void validarSolicitudPagoCuotas(PagoCuotasRequest request) {
        if (request.getCuotaIds() == null || request.getCuotaIds().isEmpty()) {
            throw new RuntimeException("Seleccioná al menos una cuota");
        }
        if (request.getMontoEntregado() == null || request.getMontoEntregado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto entregado debe ser mayor a cero");
        }
    }

    private List<Cuota> cargarCuotasValidadas(List<Long> cuotaIds) {
        List<Cuota> cuotas = cuotaRepository.findByIdInWithCredito(cuotaIds);
        if (cuotas.size() != cuotaIds.size()) {
            throw new RuntimeException("Una o más cuotas no existen");
        }

        Long clienteId = cuotas.get(0).getCredito().getCliente().getId();
        for (Cuota cuota : cuotas) {
            if (!cuota.getCredito().getCliente().getId().equals(clienteId)) {
                throw new RuntimeException("Todas las cuotas deben pertenecer al mismo cliente");
            }
            if (cuota.getEstado() == Cuota.EstadoCuota.PAGADA
                    || cuota.getEstado() == Cuota.EstadoCuota.CANCELADA
                    || cuota.getEstado() == Cuota.EstadoCuota.ELIMINADA) {
                throw new RuntimeException("La cuota " + cuota.getNumero() + " ya está cerrada");
            }
            BigDecimal saldo = cuota.getSaldo() != null ? cuota.getSaldo() : cuota.getMonto();
            if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("La cuota " + cuota.getNumero() + " no tiene saldo pendiente");
            }
        }
        return cuotas;
    }

    private BigDecimal calcularTotalPendiente(List<Cuota> cuotas) {
        return cuotas.stream()
                .map(q -> q.getSaldo() != null ? q.getSaldo() : q.getMonto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Long, BigDecimal> aplicarPagoACuotas(List<Cuota> cuotas, PagoCuotasRequest request) {
        Map<Long, BigDecimal> pagadoPorCreditoId = new HashMap<>();
        for (Cuota cuota : cuotas) {
            BigDecimal saldoPendiente = cuota.getSaldo() != null ? cuota.getSaldo() : cuota.getMonto();
            cuota.setSaldo(BigDecimal.ZERO);
            cuota.setEstado(Cuota.EstadoCuota.PAGADA);
            cuotaRepository.save(cuota);
            pagadoPorCreditoId.merge(cuota.getCredito().getId(), saldoPendiente, BigDecimal::add);
            registrarPagoCuota(cuota, saldoPendiente, request);
        }
        return pagadoPorCreditoId;
    }

    private void registrarPagoCuota(Cuota cuota, BigDecimal monto, PagoCuotasRequest request) {
        PagoCuota pagoCuota = new PagoCuota();
        pagoCuota.setCuota(cuota);
        pagoCuota.setMonto(monto);
        pagoCuota.setMetodoPago(request.getMetodoPago() != null ? request.getMetodoPago() : "EFECTIVO");
        pagoCuota.setEstado(PagoCuota.EstadoPagoCuota.ACTIVO);
        if (request.getMetodoPago() != null && request.getMetodoPago().equalsIgnoreCase("EFECTIVO")) {
            MovimientoFinanciero movimiento = registrarIngresoCaja(
                    request.getCuentaFinancieraId(),
                    monto,
                    "Cobro cuota " + cuota.getNumero() + " crédito " + cuota.getCredito().getNumero()
            );
            pagoCuota.setMovimientoFinanciero(movimiento);
        }
        pagoCuotaRepository.save(pagoCuota);
    }

    private void actualizarSaldosCreditos(Map<Long, BigDecimal> pagadoPorCreditoId) {
        for (Map.Entry<Long, BigDecimal> entry : pagadoPorCreditoId.entrySet()) {
            Credito credito = creditoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Crédito no encontrado"));
            List<Cuota> cuotasCredito = cuotaRepository.findByCreditoIdIn(List.of(credito.getId()));
            BigDecimal saldoCred = credito.getSaldo() != null ? credito.getSaldo() : BigDecimal.ZERO;
            credito.setSaldo(saldoCred.subtract(entry.getValue()).max(BigDecimal.ZERO));
            actualizarEstadoCredito(credito, cuotasCredito);
            if (credito.getSaldo().compareTo(BigDecimal.ZERO) <= 0) {
                credito.setSaldo(BigDecimal.ZERO);
            }
            creditoRepository.save(credito);
        }
    }

    private void descontarSaldoCuentaCliente(Long clienteId, Long sucursalId, BigDecimal montoAplicado) {
        Cuenta cuentaCredito = creditoCuentaRepository
                .findByClienteIdAndSucursalIdAndActivoTrue(clienteId, sucursalId)
                .orElse(null);
        if (cuentaCredito == null) {
            return;
        }
        BigDecimal saldoCuenta = cuentaCredito.getSaldoActual() != null ? cuentaCredito.getSaldoActual() : BigDecimal.ZERO;
        cuentaCredito.setSaldoActual(saldoCuenta.subtract(montoAplicado).max(BigDecimal.ZERO));
        creditoCuentaRepository.save(cuentaCredito);
    }

    private PagoCuotasResponse construirRespuestaPagoCuotas(
            PagoCuotasRequest request,
            BigDecimal totalCuotas,
            List<Cuota> cuotas
    ) {
        PagoCuotasResponse response = new PagoCuotasResponse();
        response.setTotalCuotas(totalCuotas);
        response.setMontoEntregado(request.getMontoEntregado());
        response.setMontoAplicado(totalCuotas);
        response.setCambio(request.getMontoEntregado().subtract(totalCuotas));
        response.setCuotasActualizadas(cuotas.stream().map(q -> mapCuota(q, q)).toList());
        return response;
    }

    @Transactional(readOnly = true)
    public List<PagoCuotaResponse> listarPagosPorCuenta(Long cuentaId) {
        Cuenta cuenta = creditoCuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));
        return pagoCuotaRepository.findByCuentaIdOrderByCreatedAtDesc(cuentaId).stream()
                .map(this::mapPagoCuota)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PagoCuotaResponse> listarPagosPorCuentaConBackfill(Long cuentaId) {
        Cuenta cuenta = creditoCuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));
        backfillPagosDesdeCuotasPagadas(cuenta.getCliente().getId());
        return pagoCuotaRepository.findByCuentaIdOrderByCreatedAtDesc(cuentaId).stream()
                .map(this::mapPagoCuota)
                .collect(Collectors.toList());
    }

    private void backfillPagosDesdeCuotasPagadas(Long clienteId) {
        List<Cuota> cuotasSinPago = pagoCuotaRepository.findPagadasSinPagoActivo(
                clienteId,
                Cuota.EstadoCuota.PAGADA,
                PagoCuota.EstadoPagoCuota.ACTIVO
        );
        for (Cuota cuota : cuotasSinPago) {
            BigDecimal montoCuota = cuota.getMonto() != null ? cuota.getMonto() : BigDecimal.ZERO;
            if (montoCuota.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PagoCuota pago = new PagoCuota();
            pago.setCuota(cuota);
            pago.setMonto(montoCuota);
            pago.setMetodoPago("HISTORICO");
            pago.setEstado(PagoCuota.EstadoPagoCuota.ACTIVO);
            pagoCuotaRepository.save(pago);
        }
    }

    @Transactional
    public PagoCuotaResponse anularPago(Long pagoId, String motivo) {
        PagoCuota pago = pagoCuotaRepository.findByIdWithDetalle(pagoId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
        if (pago.getEstado() == PagoCuota.EstadoPagoCuota.ANULADO) {
            throw new RuntimeException("El pago ya fue anulado");
        }

        Cuota cuota = pago.getCuota();
        Credito credito = cuota.getCredito();
        BigDecimal monto = pago.getMonto() != null ? pago.getMonto() : BigDecimal.ZERO;
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El pago no tiene monto válido");
        }
        if (cuota.getEstado() != Cuota.EstadoCuota.PAGADA) {
            throw new RuntimeException("La cuota " + cuota.getNumero() + " no está pagada");
        }

        cuota.setSaldo(monto);
        cuota.setEstado(Cuota.EstadoCuota.PENDIENTE);
        cuotaRepository.save(cuota);

        List<Cuota> cuotasCredito = cuotaRepository.findByCreditoIdIn(List.of(credito.getId()));
        BigDecimal saldoCred = credito.getSaldo() != null ? credito.getSaldo() : BigDecimal.ZERO;
        credito.setSaldo(saldoCred.add(monto));
        actualizarEstadoCredito(credito, cuotasCredito);
        creditoRepository.save(credito);

        Long clienteId = credito.getCliente().getId();
        Cuenta cuentaCredito = creditoCuentaRepository
                .findByClienteIdAndSucursalIdAndActivoTrue(clienteId, credito.getSucursal().getId())
                .orElse(null);
        if (cuentaCredito != null) {
            BigDecimal saldoCuenta = cuentaCredito.getSaldoActual() != null
                    ? cuentaCredito.getSaldoActual() : BigDecimal.ZERO;
            cuentaCredito.setSaldoActual(saldoCuenta.add(monto));
            creditoCuentaRepository.save(cuentaCredito);
        }

        if (pago.getMovimientoFinanciero() != null) {
            registrarEgresoCaja(
                    pago.getMovimientoFinanciero().getCuenta().getId(),
                    monto,
                    "Anulación cobro cuota " + cuota.getNumero()
            );
        }

        pago.setEstado(PagoCuota.EstadoPagoCuota.ANULADO);
        pago.setMotivoAnulacion(motivo);
        pago.setFechaAnulacion(LocalDateTime.now());
        PagoCuota anulado = pagoCuotaRepository.save(pago);
        return mapPagoCuota(anulado);
    }

    private PagoCuotaResponse mapPagoCuota(PagoCuota pago) {
        Cuota cuota = pago.getCuota();
        Credito credito = cuota.getCredito();
        PagoCuotaResponse dto = new PagoCuotaResponse();
        dto.setId(pago.getId());
        dto.setCuotaId(cuota.getId());
        dto.setCuotaNumero(cuota.getNumero());
        dto.setCreditoId(credito.getId());
        dto.setCreditoNumero(credito.getNumero());
        dto.setMonto(pago.getMonto());
        dto.setMetodoPago(pago.getMetodoPago());
        dto.setEstado(pago.getEstado() != null ? pago.getEstado().name() : PagoCuota.EstadoPagoCuota.ACTIVO.name());
        dto.setCreatedAt(pago.getCreatedAt());
        dto.setFechaAnulacion(pago.getFechaAnulacion());
        dto.setMotivoAnulacion(pago.getMotivoAnulacion());
        return dto;
    }

    private ClienteCreditosResponse construirClienteCreditos(Cuenta cuenta) {
        Cliente cliente = cuenta.getCliente();
        List<Credito> creditos = creditoRepository.findByClienteIdOrderByCreatedAtDesc(cliente.getId());
        Map<Long, List<Cuota>> cuotasPorCredito = cargarCuotasPorCredito(creditos);

        List<CreditoClienteResponse> activos = new ArrayList<>();
        List<CreditoClienteResponse> cancelados = new ArrayList<>();
        int idx = 1;
        for (Credito c : creditos) {
            List<Cuota> cuotas = cuotasPorCredito.getOrDefault(c.getId(), List.of());
            CreditoClienteResponse dto = mapCredito(c, cuotas, idx++);
            if (c.getEstado() == Credito.EstadoCredito.PAGADO || c.getEstado() == Credito.EstadoCredito.CANCELADO) {
                cancelados.add(dto);
            } else {
                activos.add(dto);
            }
        }

        BigDecimal saldoCuenta = creditoCuentaRepository.findById(cuenta.getId())
                .map(Cuenta::getSaldoActual)
                .orElse(cuenta.getSaldoActual());

        ClienteCreditosResponse response = new ClienteCreditosResponse();
        response.setCuentaId(cuenta.getId());
        response.setCuentaNumero(cuenta.getNumero());
        response.setClienteId(cliente.getId());
        response.setClienteNombre(cliente.getNombre());
        response.setClienteApellido(cliente.getApellido());
        response.setClienteDni(cliente.getDni());
        response.setClienteTelefono(cliente.getTelefono());
        response.setSucursalId(cuenta.getSucursal().getId());
        response.setSaldoCuenta(saldoCuenta);
        response.setLimiteCredito(cuenta.getLimiteCredito());
        aplicarResumenCreditos(response, creditos);
        response.setCreditosActivos(activos);
        response.setCreditosCancelados(cancelados);
        return response;
    }

    private Map<Long, List<Cuota>> cargarCuotasPorCredito(List<Credito> creditos) {
        if (creditos.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = creditos.stream().map(Credito::getId).collect(Collectors.toList());
        return cuotaRepository.findByCreditoIdIn(ids).stream()
                .collect(Collectors.groupingBy(q -> q.getCredito().getId()));
    }

    private CreditoClienteResponse mapCredito(Credito credito, List<Cuota> cuotas, int indice) {
        List<Cuota> cuotasOrdenadas = cuotas.stream()
                .sorted(Comparator.comparing(Cuota::getFechaVencimiento, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        CreditoClienteResponse dto = new CreditoClienteResponse();
        dto.setId(credito.getId());
        dto.setNumero(credito.getNumero());
        dto.setIndice(indice);
        dto.setImporte(credito.getImporte());
        dto.setSaldo(credito.getSaldo());
        dto.setEstado(credito.getEstado() != null ? credito.getEstado().name() : null);
        dto.setDescripcion(credito.getDescripcion());
        dto.setVentaId(credito.getVenta() != null ? credito.getVenta().getId() : null);
        dto.setNumeroFactura(credito.getVenta() != null ? credito.getVenta().getNumeroFactura() : null);
        dto.setCreatedAt(credito.getCreatedAt());
        dto.setFechaVencimiento(cuotasOrdenadas.stream()
                .filter(q -> {
                    String est = estadoCuotaEfectivo(q);
                    return "PENDIENTE".equals(est) || "VENCIDA".equals(est);
                })
                .map(Cuota::getFechaVencimiento)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(cuotasOrdenadas.isEmpty() ? null : cuotasOrdenadas.get(cuotasOrdenadas.size() - 1).getFechaVencimiento()));
        dto.setCuotas(cuotasOrdenadas.stream().map(q -> mapCuota(q, q)).collect(Collectors.toList()));
        return dto;
    }

    private String estadoCuotaEfectivo(Cuota q) {
        if (q.getEstado() == Cuota.EstadoCuota.PENDIENTE
                && q.getFechaVencimiento() != null
                && q.getFechaVencimiento().isBefore(LocalDateTime.now())) {
            return Cuota.EstadoCuota.VENCIDA.name();
        }
        return q.getEstado() != null ? q.getEstado().name() : Cuota.EstadoCuota.PENDIENTE.name();
    }

    private void actualizarEstadoCredito(Credito credito, List<Cuota> cuotas) {
        boolean tienePendientes = cuotas.stream()
                .anyMatch(q -> {
                    String est = estadoCuotaEfectivo(q);
                    return "PENDIENTE".equals(est) || "VENCIDA".equals(est);
                });
        if (!tienePendientes) {
            credito.setEstado(Credito.EstadoCredito.PAGADO);
            credito.setSaldo(BigDecimal.ZERO);
        } else {
            boolean tieneVencidas = cuotas.stream()
                    .anyMatch(q -> "VENCIDA".equals(estadoCuotaEfectivo(q)));
            credito.setEstado(tieneVencidas ? Credito.EstadoCredito.VENCIDO : Credito.EstadoCredito.ACTIVO);
        }
    }

    private static final BigDecimal PORCENTAJE_RECARGO = BigDecimal.valueOf(0.10);

    private CuotaCreditoResponse mapCuota(Cuota q, Cuota ref) {
        BigDecimal monto = ref.getMonto() != null ? ref.getMonto() : BigDecimal.ZERO;
        BigDecimal recargo = calcularRecargo(ref);
        BigDecimal saldo = ref.getSaldo() != null ? ref.getSaldo() : BigDecimal.ZERO;
        BigDecimal totalCuota = monto.add(recargo);
        BigDecimal pago = totalCuota.subtract(saldo).max(BigDecimal.ZERO);

        CuotaCreditoResponse dto = new CuotaCreditoResponse();
        dto.setId(ref.getId());
        dto.setCreditoId(ref.getCredito().getId());
        dto.setNumero(ref.getNumero());
        dto.setFechaVencimiento(ref.getFechaVencimiento());
        dto.setMonto(monto);
        dto.setPagoRealizado(pago);
        dto.setSaldo(saldo);
        dto.setEstado(estadoCuotaEfectivo(ref));
        dto.setDescripcion(ref.getDescripcion());
        dto.setRecargo(calcularRecargo(ref));
        dto.setDiasAtraso(calcularDiasAtraso(ref));
        return dto;
    }

    private BigDecimal calcularRecargo(Cuota q) {
        if (!"VENCIDA".equals(estadoCuotaEfectivo(q))) {
            return BigDecimal.ZERO;
        }
        BigDecimal monto = q.getMonto() != null ? q.getMonto() : BigDecimal.ZERO;
        return monto.multiply(PORCENTAJE_RECARGO).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private Integer calcularDiasAtraso(Cuota q) {
        if (q.getFechaVencimiento() == null) return 0;
        if (q.getEstado() == Cuota.EstadoCuota.PAGADA
                || q.getEstado() == Cuota.EstadoCuota.CANCELADA
                || q.getEstado() == Cuota.EstadoCuota.ELIMINADA) {
            return 0;
        }
        if (!"VENCIDA".equals(estadoCuotaEfectivo(q)) && q.getEstado() != Cuota.EstadoCuota.PENDIENTE) {
            return 0;
        }
        long dias = ChronoUnit.DAYS.between(q.getFechaVencimiento().toLocalDate(), LocalDateTime.now().toLocalDate());
        return (int) Math.max(0, dias);
    }

    private CuentaCreditoListResponse mapCuentaList(Cuenta cuenta) {
        Cliente cliente = cuenta.getCliente();
        BigDecimal saldoActual = creditoCuentaRepository.findById(cuenta.getId())
                .map(Cuenta::getSaldoActual)
                .orElse(cuenta.getSaldoActual());
        CuentaCreditoListResponse dto = new CuentaCreditoListResponse();
        dto.setId(cuenta.getId());
        dto.setNumero(cuenta.getNumero());
        dto.setClienteId(cliente.getId());
        dto.setClienteNombre(cliente.getNombre());
        dto.setClienteApellido(cliente.getApellido());
        dto.setClienteDni(cliente.getDni());
        dto.setClienteTelefono(cliente.getTelefono());
        dto.setSucursalId(cuenta.getSucursal().getId());
        dto.setSucursalNombre(cuenta.getSucursal().getNombre());
        dto.setSaldoActual(saldoActual);
        dto.setLimiteCredito(cuenta.getLimiteCredito());
        dto.setActivo(cuenta.getActivo());
        return dto;
    }

    @Transactional(readOnly = true)
    public DashboardCreditosResumenResponse resumenParaDashboard() {
        List<Cuenta> cuentas = creditoCuentaRepository.findAllActivasWithCliente();
        List<Long> clienteIds = cuentas.stream().map(c -> c.getCliente().getId()).distinct().toList();
        Set<Long> clientesVencidos = loadClientesConVencidos(clienteIds);
        Set<Long> clientesActivos = new HashSet<>(creditoRepository.findClienteIdsConCreditosActivos());
        Map<Long, CreditoResumenPorCliente> resumenMap = loadResumenMap(clienteIds);

        DashboardAgregado agregado = agregarCuentasDashboard(cuentas, clientesVencidos, clientesActivos, resumenMap);
        List<DashboardCuentaCreditoItemResponse> topCuentas = seleccionarTopCuentasPorSaldo(agregado.porCliente());

        DashboardCreditosResumenResponse resumen = new DashboardCreditosResumenResponse();
        resumen.setCuentasActivas(agregado.porCliente().size());
        resumen.setCuentasAlDia(agregado.cuentasAlDia());
        resumen.setCuentasConVencidos(agregado.cuentasConVencidos());
        resumen.setSaldoTotalCuentas(agregado.saldoTotal());
        resumen.setCuotasPorEstado(cargarCuotasPorEstadoOrdenadas());
        resumen.setCuentas(topCuentas);
        return resumen;
    }

    private DashboardAgregado agregarCuentasDashboard(
            List<Cuenta> cuentas,
            Set<Long> clientesVencidos,
            Set<Long> clientesActivos,
            Map<Long, CreditoResumenPorCliente> resumenMap
    ) {
        BigDecimal saldoTotal = BigDecimal.ZERO;
        long cuentasConVencidos = 0;
        long cuentasAlDia = 0;
        Set<Long> clientesContadosVencidos = new HashSet<>();
        Set<Long> clientesContadosAlDia = new HashSet<>();
        Map<Long, DashboardCuentaCreditoItemResponse> porCliente = new LinkedHashMap<>();

        for (Cuenta cuenta : cuentas) {
            BigDecimal saldo = cuenta.getSaldoActual() != null ? cuenta.getSaldoActual() : BigDecimal.ZERO;
            saldoTotal = saldoTotal.add(saldo);
            Long clienteId = cuenta.getCliente().getId();
            boolean vencidos = clientesVencidos.contains(clienteId);
            if (vencidos && clientesContadosVencidos.add(clienteId)) {
                cuentasConVencidos++;
            } else if (!vencidos && clientesActivos.contains(clienteId) && clientesContadosAlDia.add(clienteId)) {
                cuentasAlDia++;
            }
            incorporarCuentaEnAgregado(porCliente, cuenta, saldo, vencidos, resumenMap.get(clienteId));
        }
        return new DashboardAgregado(porCliente, saldoTotal, cuentasAlDia, cuentasConVencidos);
    }

    private void incorporarCuentaEnAgregado(
            Map<Long, DashboardCuentaCreditoItemResponse> porCliente,
            Cuenta cuenta,
            BigDecimal saldo,
            boolean vencidos,
            CreditoResumenPorCliente resumen
    ) {
        Long clienteId = cuenta.getCliente().getId();
        int cantCreditos = resumen != null && resumen.getCantidadCreditos() != null
                ? resumen.getCantidadCreditos().intValue() : 0;

        Cliente cliente = cuenta.getCliente();
        String nombreCliente = ((cliente.getNombre() != null ? cliente.getNombre() : "") + " "
                + (cliente.getApellido() != null ? cliente.getApellido() : "")).trim();

        DashboardCuentaCreditoItemResponse existente = porCliente.get(clienteId);
        if (existente == null) {
            if (saldo.compareTo(BigDecimal.ZERO) > 0 || vencidos || cantCreditos > 0) {
                porCliente.put(clienteId, new DashboardCuentaCreditoItemResponse(
                        cuenta.getId(), cuenta.getNumero(), nombreCliente, cliente.getDni(),
                        saldo, vencidos, cantCreditos));
            }
            return;
        }
        existente.setSaldoActual(existente.getSaldoActual().add(saldo));
        existente.setTieneCreditosVencidos(existente.isTieneCreditosVencidos() || vencidos);
    }

    private List<DashboardCuentaCreditoItemResponse> seleccionarTopCuentasPorSaldo(
            Map<Long, DashboardCuentaCreditoItemResponse> porCliente
    ) {
        List<DashboardCuentaCreditoItemResponse> items = new ArrayList<>(porCliente.values());
        items.sort(Comparator.comparing(DashboardCuentaCreditoItemResponse::getSaldoActual).reversed());
        if (items.size() <= 8) {
            return items;
        }
        return new ArrayList<>(items.subList(0, 8));
    }

    private List<DashboardCuotaPorEstadoResponse> cargarCuotasPorEstadoOrdenadas() {
        Map<String, DashboardCuotaPorEstadoResponse> porEstado = dashboardQueryRepository.resumenCuotasPorEstado()
                .stream()
                .collect(Collectors.toMap(DashboardCuotaPorEstadoResponse::getEstado, e -> e, (a, b) -> a));
        List<DashboardCuotaPorEstadoResponse> resultado = new ArrayList<>();
        for (Cuota.EstadoCuota estado : Cuota.EstadoCuota.values()) {
            resultado.add(porEstado.getOrDefault(
                    estado.name(),
                    new DashboardCuotaPorEstadoResponse(estado.name(), 0, BigDecimal.ZERO)
            ));
        }
        return resultado;
    }

    private record DashboardAgregado(
            Map<Long, DashboardCuentaCreditoItemResponse> porCliente,
            BigDecimal saldoTotal,
            long cuentasAlDia,
            long cuentasConVencidos
    ) {
    }

    private MovimientoFinanciero registrarIngresoCaja(Long cuentaFinancieraId, BigDecimal monto, String descripcion) {
        return cajaMovimientoService.registrarIngreso(cuentaFinancieraId, monto, descripcion);
    }

    private void registrarEgresoCaja(Long cuentaFinancieraId, BigDecimal monto, String descripcion) {
        cajaMovimientoService.registrarEgreso(cuentaFinancieraId, monto, descripcion);
    }
}
