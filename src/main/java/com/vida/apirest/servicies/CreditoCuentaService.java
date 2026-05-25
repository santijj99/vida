package com.vida.apirest.servicies;

import com.vida.apirest.dto.credito.*;
import com.vida.apirest.model.credito.Credito;
import com.vida.apirest.model.credito.Cuota;
import com.vida.apirest.model.credito.Cuenta;
import com.vida.apirest.model.finanzas.CuentaFinanciera;
import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.model.tesoreria.MovimientoFinanciero;
import com.vida.apirest.repositories.CreditoRepository;
import com.vida.apirest.repositories.CuotaRepository;
import com.vida.apirest.repositories.CuentaRepository;
import com.vida.apirest.repositories.FinanzasCuentaFinancieraRepository;
import com.vida.apirest.repositories.MovimientoFinancieroRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional(readOnly = true)
    public List<CuentaCreditoListResponse> listarCuentas(Long sucursalId) {
        List<Cuenta> cuentas = sucursalId != null
                ? creditoCuentaRepository.findActivasBySucursalWithCliente(sucursalId)
                : creditoCuentaRepository.findAllActivasWithCliente();
        return cuentas.stream().map(this::mapCuentaList).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClienteCreditosResponse obtenerCreditosPorCuenta(Long cuentaId) {
        Cuenta cuenta = creditoCuentaRepository.findByIdWithCliente(cuentaId)
                .orElseThrow(() -> new RuntimeException("Cuenta de crédito no encontrada"));
        return construirClienteCreditos(cuenta);
    }

    @Transactional(readOnly = true)
    public ClienteCreditosResponse obtenerCreditosPorCliente(Long clienteId) {
        Cuenta cuenta = creditoCuentaRepository.findActivaByClienteId(clienteId)
                .orElseThrow(() -> new RuntimeException("Cuenta de crédito no encontrada para el cliente"));
        return construirClienteCreditos(cuenta);
    }

    @Transactional
    public PagoCuotasResponse pagarCuotas(PagoCuotasRequest request) {
        if (request.getCuotaIds() == null || request.getCuotaIds().isEmpty()) {
            throw new RuntimeException("Seleccioná al menos una cuota");
        }
        if (request.getMontoEntregado() == null || request.getMontoEntregado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto entregado debe ser mayor a cero");
        }

        List<Cuota> cuotas = cuotaRepository.findByIdInWithCredito(request.getCuotaIds());
        if (cuotas.size() != request.getCuotaIds().size()) {
            throw new RuntimeException("Una o más cuotas no existen");
        }

        Long clienteId = cuotas.get(0).getCredito().getCliente().getId();
        for (Cuota q : cuotas) {
            if (!q.getCredito().getCliente().getId().equals(clienteId)) {
                throw new RuntimeException("Todas las cuotas deben pertenecer al mismo cliente");
            }
            if (q.getEstado() == Cuota.EstadoCuota.PAGADA || q.getEstado() == Cuota.EstadoCuota.CANCELADA) {
                throw new RuntimeException("La cuota " + q.getNumero() + " ya está cerrada");
            }
            BigDecimal saldo = q.getSaldo() != null ? q.getSaldo() : q.getMonto();
            if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("La cuota " + q.getNumero() + " no tiene saldo pendiente");
            }
        }

        BigDecimal totalCuotas = cuotas.stream()
                .map(q -> q.getSaldo() != null ? q.getSaldo() : q.getMonto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (request.getMontoEntregado().compareTo(totalCuotas) < 0) {
            throw new RuntimeException("El monto entregado es menor al total de las cuotas seleccionadas");
        }

        Map<Long, BigDecimal> pagadoPorCreditoId = new HashMap<>();
        List<CuotaCreditoResponse> actualizadas = new ArrayList<>();

        for (Cuota q : cuotas) {
            BigDecimal saldoPendiente = q.getSaldo() != null ? q.getSaldo() : q.getMonto();
            q.setSaldo(BigDecimal.ZERO);
            q.setEstado(Cuota.EstadoCuota.PAGADA);
            cuotaRepository.save(q);
            pagadoPorCreditoId.merge(q.getCredito().getId(), saldoPendiente, BigDecimal::add);
            actualizadas.add(mapCuota(q, q));
        }

        BigDecimal montoAplicado = totalCuotas;
        for (Map.Entry<Long, BigDecimal> entry : pagadoPorCreditoId.entrySet()) {
            Credito credito = creditoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Crédito no encontrado"));
            List<Cuota> cuotasCredito = cuotaRepository.findByCreditoIdIn(List.of(credito.getId()));
            BigDecimal pagado = entry.getValue();
            BigDecimal saldoCred = credito.getSaldo() != null ? credito.getSaldo() : BigDecimal.ZERO;
            credito.setSaldo(saldoCred.subtract(pagado).max(BigDecimal.ZERO));
            actualizarEstadoCredito(credito, cuotasCredito);
            if (credito.getSaldo().compareTo(BigDecimal.ZERO) <= 0) {
                credito.setSaldo(BigDecimal.ZERO);
            }
            creditoRepository.save(credito);
        }

        Cuenta cuentaCredito = creditoCuentaRepository
                .findByClienteIdAndSucursalIdAndActivoTrue(clienteId, cuotas.get(0).getCredito().getSucursal().getId())
                .orElse(null);
        if (cuentaCredito != null) {
            BigDecimal saldoCuenta = cuentaCredito.getSaldoActual() != null ? cuentaCredito.getSaldoActual() : BigDecimal.ZERO;
            cuentaCredito.setSaldoActual(saldoCuenta.subtract(montoAplicado).max(BigDecimal.ZERO));
            creditoCuentaRepository.save(cuentaCredito);
        }

        if (request.getMetodoPago() != null && request.getMetodoPago().equalsIgnoreCase("EFECTIVO")) {
            registrarIngresoCaja(request.getCuentaFinancieraId(), montoAplicado, "Cobro cuotas crédito");
        }

        PagoCuotasResponse response = new PagoCuotasResponse();
        response.setTotalCuotas(totalCuotas);
        response.setMontoEntregado(request.getMontoEntregado());
        response.setMontoAplicado(montoAplicado);
        response.setCambio(request.getMontoEntregado().subtract(montoAplicado));
        response.setCuotasActualizadas(actualizadas);
        return response;
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
        response.setSucursalId(cuenta.getSucursal().getId());
        response.setSaldoCuenta(saldoCuenta);
        response.setLimiteCredito(cuenta.getLimiteCredito());
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

    private CuotaCreditoResponse mapCuota(Cuota q, Cuota ref) {
        BigDecimal monto = ref.getMonto() != null ? ref.getMonto() : BigDecimal.ZERO;
        BigDecimal saldo = ref.getSaldo() != null ? ref.getSaldo() : BigDecimal.ZERO;
        BigDecimal pago = monto.subtract(saldo).max(BigDecimal.ZERO);

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
        BigDecimal saldo = q.getSaldo() != null ? q.getSaldo() : q.getMonto();
        return saldo.multiply(BigDecimal.valueOf(0.02)).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private Integer calcularDiasAtraso(Cuota q) {
        if (q.getFechaVencimiento() == null) return 0;
        if (q.getEstado() == Cuota.EstadoCuota.PAGADA || q.getEstado() == Cuota.EstadoCuota.CANCELADA) {
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
        dto.setSucursalId(cuenta.getSucursal().getId());
        dto.setSucursalNombre(cuenta.getSucursal().getNombre());
        dto.setSaldoActual(saldoActual);
        dto.setLimiteCredito(cuenta.getLimiteCredito());
        dto.setActivo(cuenta.getActivo());
        return dto;
    }

    private void registrarIngresoCaja(Long cuentaFinancieraId, BigDecimal monto, String descripcion) {
        CuentaFinanciera cuenta = null;
        if (cuentaFinancieraId != null) {
            cuenta = cuentaFinancieraRepository.findById(cuentaFinancieraId)
                    .orElseThrow(() -> new RuntimeException("Cuenta financiera no encontrada"));
        } else {
            cuenta = cuentaFinancieraRepository.findFirstByTipoAndActivoTrue(CuentaFinanciera.TipoCuenta.CAJA)
                    .orElse(null);
        }
        if (cuenta == null) return;

        BigDecimal saldoAnterior = cuenta.getSaldoActual() != null ? cuenta.getSaldoActual() : BigDecimal.ZERO;
        BigDecimal saldoNuevo = saldoAnterior.add(monto);
        cuenta.setSaldoActual(saldoNuevo);
        cuentaFinancieraRepository.save(cuenta);

        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setCuenta(cuenta);
        movimiento.setNumero("MV-" + UUID.randomUUID().toString().replace("-", ""));
        movimiento.setTipo(MovimientoFinanciero.TipoMovimiento.INGRESO);
        movimiento.setMonto(monto);
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoNuevo(saldoNuevo);
        movimiento.setDescripcion(descripcion);
        movimiento.setResponsable("sistema");
        movimientoFinancieroRepository.save(movimiento);
    }
}
