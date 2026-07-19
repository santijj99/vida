package com.vida.apirest.servicies;

import com.vida.apirest.dto.credito.CreditoClienteResponse;
import com.vida.apirest.dto.credito.CuotaEdicionRequest;
import com.vida.apirest.dto.credito.EditarCreditoRequest;
import com.vida.apirest.model.credito.Credito;
import com.vida.apirest.model.credito.Cuota;
import com.vida.apirest.repositories.CreditoRepository;
import com.vida.apirest.repositories.CuotaRepository;
import com.vida.apirest.security.SucursalScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditoRefinanciacionService {

    private final CreditoRepository creditoRepository;
    private final CuotaRepository cuotaRepository;
    private final CreditoHistorialService historialService;
    private final CreditoRecargoService recargoService;
    private final CreditoEstadoService estadoService;
    private final SucursalScopeService sucursalScopeService;

    @Transactional
    public CreditoClienteResponse editarCredito(Long creditoId, EditarCreditoRequest request) {
        Credito credito = creditoRepository.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado"));
        sucursalScopeService.assertCanAccess(credito.getSucursal().getId());

        if (credito.getEstado() == Credito.EstadoCredito.CANCELADO
                || credito.getEstado() == Credito.EstadoCredito.PAGADO) {
            throw new RuntimeException("No se puede editar un crédito cerrado");
        }

        List<Cuota> cuotasExistentes = cuotaRepository.findByCreditoIdIn(List.of(creditoId));
        Map<Long, Cuota> porId = cuotasExistentes.stream()
                .collect(Collectors.toMap(Cuota::getId, q -> q));

        for (Cuota cuota : cuotasExistentes) {
            recargoService.aplicarRecargoIdempotente(cuota);
        }

        if (request.getDescripcion() != null
                && !Objects.equals(request.getDescripcion(), credito.getDescripcion())) {
            historialService.registrar(credito, "Descripción",
                    credito.getDescripcion(), request.getDescripcion());
            credito.setDescripcion(request.getDescripcion());
        }

        if (request.getPlazoMeses() != null && !request.getPlazoMeses().equals(credito.getPlazoMeses())) {
            historialService.registrar(credito, "Cantidad de cuotas",
                    String.valueOf(credito.getPlazoMeses()), String.valueOf(request.getPlazoMeses()));
            credito.setPlazoMeses(request.getPlazoMeses());
        }

        if (request.getCuotas() != null) {
            for (CuotaEdicionRequest ed : request.getCuotas()) {
                if (ed.getId() != null) {
                    Cuota cuota = porId.get(ed.getId());
                    if (cuota == null) {
                        throw new RuntimeException("Cuota no encontrada: " + ed.getId());
                    }
                    aplicarEdicionCuota(credito, cuota, ed);
                }
            }
        }

        recalcularCredito(credito);
        creditoRepository.save(credito);

        List<Cuota> cuotasActualizadas = cuotaRepository.findByCreditoIdIn(List.of(creditoId));
        return mapCreditoBasico(credito, cuotasActualizadas);
    }

    @Transactional
    public void quitarRecargoCuota(Long cuotaId) {
        Cuota cuota = cuotaRepository.findById(cuotaId)
                .orElseThrow(() -> new RuntimeException("Cuota no encontrada"));
        sucursalScopeService.assertCanAccess(cuota.getCredito().getSucursal().getId());
        if (!estadoService.cuotaImpaga(cuota)) {
            throw new RuntimeException("La cuota no tiene recargo pendiente");
        }
        BigDecimal anterior = estadoService.recargoPersistido(cuota);
        if (anterior.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La cuota no tiene recargo aplicado");
        }
        recargoService.quitarRecargo(cuota);
        historialService.registrar(cuota.getCredito(), "Recargo eliminado",
                "$" + anterior, "Cuota " + cuota.getNumero());
    }

    private void aplicarEdicionCuota(Credito credito, Cuota cuota, CuotaEdicionRequest ed) {
        if (Boolean.TRUE.equals(ed.getQuitarRecargo())) {
            BigDecimal anterior = estadoService.recargoPersistido(cuota);
            if (anterior.compareTo(BigDecimal.ZERO) > 0) {
                recargoService.quitarRecargo(cuota);
                historialService.registrar(credito, "Recargo eliminado",
                        "$" + anterior, "Cuota " + cuota.getNumero());
            }
            return;
        }

        if (cuota.getEstado() == Cuota.EstadoCuota.PAGADA && ed.getSaldo() != null
                && ed.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("No se puede asignar saldo a la cuota pagada " + cuota.getNumero());
        }

        if (ed.getMonto() != null && !ed.getMonto().equals(cuota.getMonto())) {
            validarNoReduceDebajoPagado(cuota, ed.getMonto());
            historialService.registrar(credito, "Monto cuota " + cuota.getNumero(),
                    fmt(cuota.getMonto()), fmt(ed.getMonto()));
            cuota.setMonto(ed.getMonto());
        }

        if (ed.getSaldo() != null && !ed.getSaldo().equals(cuota.getSaldo())) {
            BigDecimal pagado = pagadoEnCuota(cuota);
            BigDecimal monto = cuota.getMonto() != null ? cuota.getMonto() : BigDecimal.ZERO;
            if (ed.getSaldo().add(pagado).compareTo(monto) > 0) {
                throw new RuntimeException("El saldo de la cuota " + cuota.getNumero()
                        + " no puede superar el monto menos lo ya pagado");
            }
            historialService.registrar(credito, "Saldo cuota " + cuota.getNumero(),
                    fmt(cuota.getSaldo()), fmt(ed.getSaldo()));
            cuota.setSaldo(ed.getSaldo());
            if (ed.getSaldo().compareTo(BigDecimal.ZERO) <= 0
                    && cuota.getEstado() != Cuota.EstadoCuota.CANCELADA) {
                cuota.setEstado(Cuota.EstadoCuota.PAGADA);
            }
        }

        if (ed.getFechaVencimiento() != null
                && !ed.getFechaVencimiento().equals(cuota.getFechaVencimiento())) {
            historialService.registrar(credito, "Fecha cuota " + cuota.getNumero(),
                    fmtFecha(cuota.getFechaVencimiento()), fmtFecha(ed.getFechaVencimiento()));
            cuota.setFechaVencimiento(ed.getFechaVencimiento());
        }

        if (ed.getEstado() != null && !ed.getEstado().isBlank()) {
            Cuota.EstadoCuota nuevo = parseEstado(ed.getEstado());
            if (cuota.getEstado() != nuevo) {
                historialService.registrar(credito, "Estado cuota " + cuota.getNumero(),
                        cuota.getEstado().name(), nuevo.name());
                cuota.setEstado(nuevo);
            }
        }

        cuotaRepository.save(cuota);
    }

    private void validarNoReduceDebajoPagado(Cuota cuota, BigDecimal nuevoMonto) {
        BigDecimal pagado = pagadoEnCuota(cuota);
        if (nuevoMonto.compareTo(pagado) < 0) {
            throw new RuntimeException("El monto de la cuota " + cuota.getNumero()
                    + " no puede ser menor a lo ya pagado ($" + pagado + ")");
        }
        BigDecimal saldo = cuota.getSaldo() != null ? cuota.getSaldo() : BigDecimal.ZERO;
        if (nuevoMonto.subtract(pagado).compareTo(saldo) < 0) {
            cuota.setSaldo(nuevoMonto.subtract(pagado));
        }
    }

    private BigDecimal pagadoEnCuota(Cuota cuota) {
        BigDecimal monto = cuota.getMonto() != null ? cuota.getMonto() : BigDecimal.ZERO;
        BigDecimal saldo = cuota.getSaldo() != null ? cuota.getSaldo() : BigDecimal.ZERO;
        return monto.subtract(saldo).max(BigDecimal.ZERO);
    }

    private void recalcularCredito(Credito credito) {
        List<Cuota> cuotas = cuotaRepository.findByCreditoIdIn(List.of(credito.getId()));
        BigDecimal saldoTotal = cuotas.stream()
                .filter(estadoService::cuotaImpaga)
                .map(q -> estadoService.saldoCapital(q).add(recargoService.getRecargoEfectivo(q)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        credito.setSaldo(saldoTotal);

        boolean tieneVencidas = cuotas.stream()
                .anyMatch(q -> "VENCIDA".equals(estadoService.estadoCuotaEfectivo(q)));
        if (saldoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            credito.setEstado(Credito.EstadoCredito.PAGADO);
            credito.setSaldo(BigDecimal.ZERO);
        } else {
            credito.setEstado(tieneVencidas ? Credito.EstadoCredito.VENCIDO : Credito.EstadoCredito.ACTIVO);
        }
    }

    private CreditoClienteResponse mapCreditoBasico(Credito credito, List<Cuota> cuotas) {
        CreditoClienteResponse dto = new CreditoClienteResponse();
        dto.setId(credito.getId());
        dto.setNumero(credito.getNumero());
        dto.setImporte(credito.getImporte());
        dto.setSaldo(credito.getSaldo());
        dto.setEstado(credito.getEstado() != null ? credito.getEstado().name() : null);
        dto.setDescripcion(credito.getDescripcion());
        dto.setDiasAtraso(estadoService.diasAtrasoCredito(cuotas));
        dto.setRecargoAcumulado(recargoService.recargoAcumuladoCredito(cuotas));
        return dto;
    }

    private Cuota.EstadoCuota parseEstado(String estado) {
        try {
            return Cuota.EstadoCuota.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Estado de cuota inválido: " + estado);
        }
    }

    private String fmt(BigDecimal v) {
        return v != null ? "$" + v : "-";
    }

    private String fmtFecha(java.time.LocalDateTime d) {
        if (d == null) return "-";
        return String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }
}
