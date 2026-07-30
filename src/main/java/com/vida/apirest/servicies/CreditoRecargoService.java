package com.vida.apirest.servicies;

import com.vida.apirest.model.credito.Credito;
import com.vida.apirest.model.credito.CreditoConfigEmpresa;
import com.vida.apirest.model.credito.Cuota;
import com.vida.apirest.repositories.CreditoRepository;
import com.vida.apirest.repositories.CuotaRepository;
import com.vida.apirest.repositories.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditoRecargoService {

    private final CreditoEstadoService estadoService;
    private final CreditoConfigService configService;
    private final CuotaRepository cuotaRepository;
    private final CreditoRepository creditoRepository;
    private final SucursalRepository sucursalRepository;

    /** Aplica recargo de forma idempotente: solo si aún no existe y corresponde por mora. */
    @Transactional
    public void aplicarRecargoIdempotente(Cuota cuota) {
        CreditoConfigEmpresa config = resolverConfig(cuota);
        aplicarRecargoIdempotente(cuota, config);
    }

    @Transactional
    public void aplicarRecargoIdempotente(Cuota cuota, CreditoConfigEmpresa config) {
        if (cuota == null || config == null) {
            return;
        }
        BigDecimal actual = estadoService.recargoPersistido(cuota);
        if (actual.compareTo(BigDecimal.ZERO) > 0) {
            return;
        }
        if (!estadoService.debeGenerarRecargo(cuota, config)) {
            return;
        }
        BigDecimal recargo = calcularRecargoTeorico(cuota, config);
        if (recargo.compareTo(BigDecimal.ZERO) > 0) {
            cuota.setRecargo(recargo);
            cuotaRepository.save(cuota);
        }
    }

    @Transactional
    public void aplicarRecargosCredito(Credito credito) {
        CreditoConfigEmpresa config = resolverConfig(credito);
        List<Cuota> cuotas = cuotaRepository.findByCreditoIdIn(List.of(credito.getId()));
        for (Cuota cuota : cuotas) {
            aplicarRecargoIdempotente(cuota, config);
        }
    }

    @Transactional
    public void recalcularRecargosPendientesEmpresa(Long empresaId) {
        List<Credito> creditos = creditoRepository.findActivosByEmpresaId(empresaId);
        CreditoConfigEmpresa config = configService.obtenerODefault(empresaId);
        for (Credito credito : creditos) {
            List<Cuota> cuotas = cuotaRepository.findByCreditoIdIn(List.of(credito.getId()));
            for (Cuota cuota : cuotas) {
                if (!estadoService.cuotaImpaga(cuota) || Boolean.TRUE.equals(cuota.getRecargoExento())) {
                    continue;
                }
                BigDecimal teorico = calcularRecargoTeorico(cuota, config);
                if (teorico.compareTo(BigDecimal.ZERO) <= 0) {
                    cuota.setRecargo(BigDecimal.ZERO);
                } else {
                    cuota.setRecargo(teorico);
                }
                cuotaRepository.save(cuota);
            }
        }
    }

    @Transactional
    public void quitarRecargo(Cuota cuota) {
        cuota.setRecargo(BigDecimal.ZERO);
        cuota.setRecargoExento(true);
        cuotaRepository.save(cuota);
    }

    public BigDecimal getRecargoEfectivo(Cuota cuota) {
        return estadoService.recargoPersistido(cuota);
    }

    public BigDecimal calcularRecargoTeorico(Cuota cuota, CreditoConfigEmpresa config) {
        if (!estadoService.debeGenerarRecargo(cuota, config)) {
            return BigDecimal.ZERO;
        }
        BigDecimal capital = estadoService.saldoCapital(cuota).signum() > 0
                ? estadoService.saldoCapital(cuota)
                : (cuota.getMonto() != null ? cuota.getMonto() : BigDecimal.ZERO);
        BigDecimal pct = config.getPorcentajeMora() != null ? config.getPorcentajeMora() : BigDecimal.ZERO;
        if (config.getTipoInteres() == CreditoConfigEmpresa.TipoInteresMora.ACUMULATIVO) {
            // Reservado para futuro: por ahora igual que fijo
            return capital.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return capital.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal saldoTotalPendiente(Cuota cuota) {
        return estadoService.saldoCapital(cuota).add(getRecargoEfectivo(cuota));
    }

    public BigDecimal recargoAcumuladoCredito(List<Cuota> cuotas) {
        return cuotas.stream()
                .filter(estadoService::cuotaImpaga)
                .map(this::getRecargoEfectivo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CreditoConfigEmpresa resolverConfig(Cuota cuota) {
        if (cuota.getCredito() == null || cuota.getCredito().getSucursal() == null) {
            return configService.obtenerODefault(null);
        }
        return resolverConfig(cuota.getCredito());
    }

    private CreditoConfigEmpresa resolverConfig(Credito credito) {
        Long empresaId = sucursalRepository.findById(credito.getSucursal().getId())
                .map(s -> s.getEmpresa().getId())
                .orElse(null);
        return configService.obtenerODefault(empresaId);
    }
}
