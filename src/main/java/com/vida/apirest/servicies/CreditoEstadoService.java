package com.vida.apirest.servicies;

import com.vida.apirest.model.credito.CreditoConfigEmpresa;
import com.vida.apirest.model.credito.Cuota;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class CreditoEstadoService {

    public String estadoCuotaEfectivo(Cuota q) {
        if (q.getEstado() == Cuota.EstadoCuota.PAGADA
                || q.getEstado() == Cuota.EstadoCuota.CANCELADA
                || q.getEstado() == Cuota.EstadoCuota.ELIMINADA) {
            return q.getEstado().name();
        }
        if (q.getEstado() == Cuota.EstadoCuota.PENDIENTE
                && q.getFechaVencimiento() != null
                && q.getFechaVencimiento().isBefore(LocalDateTime.now())) {
            return Cuota.EstadoCuota.VENCIDA.name();
        }
        return q.getEstado() != null ? q.getEstado().name() : Cuota.EstadoCuota.PENDIENTE.name();
    }

    public boolean cuotaImpaga(Cuota q) {
        String est = estadoCuotaEfectivo(q);
        return "PENDIENTE".equals(est) || "VENCIDA".equals(est);
    }

    public boolean cuotaAbierta(Cuota q) {
        return cuotaImpaga(q) && saldoCapital(q).signum() > 0;
    }

    public java.math.BigDecimal saldoCapital(Cuota q) {
        return q.getSaldo() != null ? q.getSaldo() : java.math.BigDecimal.ZERO;
    }

    public java.math.BigDecimal recargoPersistido(Cuota q) {
        return q.getRecargo() != null ? q.getRecargo() : java.math.BigDecimal.ZERO;
    }

    /** Días desde el vencimiento de la cuota (sin considerar gracia). */
    public int diasDesdeVencimiento(Cuota q) {
        if (q.getFechaVencimiento() == null) {
            return 0;
        }
        if (!cuotaImpaga(q)) {
            return 0;
        }
        long dias = ChronoUnit.DAYS.between(q.getFechaVencimiento().toLocalDate(), LocalDate.now());
        return (int) Math.max(0, dias);
    }

    /** Días de atraso del crédito: desde la primera cuota impaga vencida. */
    public int diasAtrasoCredito(List<Cuota> cuotas) {
        return cuotas.stream()
                .filter(this::cuotaImpaga)
                .filter(q -> q.getFechaVencimiento() != null
                        && q.getFechaVencimiento().isBefore(LocalDateTime.now()))
                .min(Comparator.comparing(Cuota::getFechaVencimiento))
                .map(this::diasDesdeVencimiento)
                .orElse(0);
    }

    public boolean debeGenerarRecargo(Cuota q, CreditoConfigEmpresa config) {
        if (!cuotaImpaga(q) || Boolean.TRUE.equals(q.getRecargoExento())) {
            return false;
        }
        if (q.getFechaVencimiento() == null) {
            return false;
        }
        int gracia = config != null && config.getDiasGracia() != null ? config.getDiasGracia() : 0;
        LocalDate limiteGracia = q.getFechaVencimiento().toLocalDate().plusDays(gracia);
        return LocalDate.now().isAfter(limiteGracia);
    }
}
