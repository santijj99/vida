package com.vida.apirest.servicies;

import com.vida.apirest.dto.venta.AbrirCajaRequest;
import com.vida.apirest.dto.venta.CajaMovimientoResponse;
import com.vida.apirest.dto.venta.CajaSesionResponse;
import com.vida.apirest.dto.venta.CerrarCajaRequest;
import com.vida.apirest.model.finanzas.CajaSesion;
import com.vida.apirest.model.finanzas.CuentaFinanciera;
import com.vida.apirest.model.tesoreria.MovimientoFinanciero;
import com.vida.apirest.repositories.CajaSesionRepository;
import com.vida.apirest.repositories.FinanzasCuentaFinancieraRepository;
import com.vida.apirest.repositories.MovimientoFinancieroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CajaSesionService {

    private final CajaSesionRepository cajaSesionRepository;
    private final FinanzasCuentaFinancieraRepository cuentaFinancieraRepository;
    private final MovimientoFinancieroRepository movimientoFinancieroRepository;

    @Transactional(readOnly = true)
    public CajaSesionResponse obtenerSesionActiva(Long cuentaId) {
        return cajaSesionRepository
                .findAbiertaByCuentaId(cuentaId, CajaSesion.EstadoSesion.ABIERTA)
                .map(s -> mapResponse(s, true))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CajaSesionResponse> listarSesiones(Long cuentaId) {
        return cajaSesionRepository.findByCuentaIdOrderByFechaAperturaDesc(cuentaId).stream()
                .map(s -> mapResponse(s, s.getEstado() == CajaSesion.EstadoSesion.ABIERTA))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CajaMovimientoResponse> listarMovimientosSesion(Long sesionId) {
        CajaSesion sesion = cajaSesionRepository.findByIdWithCuenta(sesionId)
                .orElseThrow(() -> new RuntimeException("Sesión de caja no encontrada"));
        LocalDateTime desde = sesion.getFechaApertura();
        LocalDateTime hasta = sesion.getFechaCierre() != null
                ? sesion.getFechaCierre()
                : LocalDateTime.now();
        List<MovimientoFinanciero> movimientos = new ArrayList<>(
                movimientoFinancieroRepository.findByCuentaIdAndCreatedAtBetween(
                        sesion.getCuenta().getId(), desde, hasta));
        Collections.reverse(movimientos);
        return movimientos.stream().map(this::mapMovimiento).toList();
    }

    @Transactional
    public CajaSesionResponse abrirCaja(AbrirCajaRequest request) {
        if (request.getCuentaId() == null) {
            throw new RuntimeException("Debe indicar la caja");
        }
        BigDecimal montoApertura = request.getMontoApertura() != null
                ? request.getMontoApertura()
                : BigDecimal.ZERO;
        if (montoApertura.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("El fondo inicial no puede ser negativo");
        }

        CuentaFinanciera cuenta = cuentaFinancieraRepository.findById(request.getCuentaId())
                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));
        if (cuenta.getTipo() != CuentaFinanciera.TipoCuenta.CAJA) {
            throw new RuntimeException("La cuenta seleccionada no es una caja");
        }
        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            throw new RuntimeException("La caja no está activa");
        }

        cajaSesionRepository
                .findAbiertaByCuentaId(cuenta.getId(), CajaSesion.EstadoSesion.ABIERTA)
                .ifPresent(s -> {
                    throw new RuntimeException(
                            "Ya hay una caja abierta para \"" + cuenta.getNombre()
                                    + "\". Cerrala antes de abrir otra.");
                });

        CajaSesion sesion = new CajaSesion();
        sesion.setCuenta(cuenta);
        sesion.setFechaApertura(LocalDateTime.now());
        sesion.setMontoApertura(montoApertura);
        sesion.setEstado(CajaSesion.EstadoSesion.ABIERTA);
        sesion.setAbiertoPor(usuarioActual());
        sesion = cajaSesionRepository.save(sesion);
        return mapResponse(sesion, true);
    }

    @Transactional
    public CajaSesionResponse cerrarCaja(Long sesionId, CerrarCajaRequest request) {
        if (request.getMontoContado() == null) {
            throw new RuntimeException("Debe indicar el efectivo contado al cerrar");
        }
        if (request.getMontoContado().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("El monto contado no puede ser negativo");
        }

        CajaSesion sesion = cajaSesionRepository.findByIdWithCuenta(sesionId)
                .orElseThrow(() -> new RuntimeException("Sesión de caja no encontrada"));
        if (sesion.getEstado() != CajaSesion.EstadoSesion.ABIERTA) {
            throw new RuntimeException("Esta caja ya está cerrada");
        }

        LocalDateTime hasta = LocalDateTime.now();
        ArqueoResumen arqueo = calcularArqueo(
                sesion.getCuenta().getId(),
                sesion.getFechaApertura(),
                hasta,
                sesion.getMontoApertura());

        BigDecimal diferencia = request.getMontoContado().subtract(arqueo.montoEsperado);

        sesion.setFechaCierre(hasta);
        sesion.setTotalIngresos(arqueo.totalIngresos);
        sesion.setTotalEgresos(arqueo.totalEgresos);
        sesion.setMontoEsperadoCierre(arqueo.montoEsperado);
        sesion.setMontoContadoCierre(request.getMontoContado());
        sesion.setDiferencia(diferencia);
        sesion.setEstado(CajaSesion.EstadoSesion.CERRADA);
        sesion.setCerradoPor(usuarioActual());
        sesion.setObservacionesCierre(request.getObservaciones());
        sesion = cajaSesionRepository.save(sesion);
        return mapResponse(sesion, false);
    }

    private CajaSesionResponse mapResponse(CajaSesion sesion, boolean recalcularSiAbierta) {
        CajaSesionResponse r = new CajaSesionResponse();
        r.setId(sesion.getId());
        r.setCuentaId(sesion.getCuenta().getId());
        r.setCuentaNombre(sesion.getCuenta().getNombre());
        r.setCuentaNumero(sesion.getCuenta().getNumero());
        r.setFechaApertura(sesion.getFechaApertura());
        r.setMontoApertura(sesion.getMontoApertura());
        r.setFechaCierre(sesion.getFechaCierre());
        r.setEstado(sesion.getEstado().name());
        r.setAbiertoPor(sesion.getAbiertoPor());
        r.setCerradoPor(sesion.getCerradoPor());
        r.setObservacionesCierre(sesion.getObservacionesCierre());

        if (recalcularSiAbierta && sesion.getEstado() == CajaSesion.EstadoSesion.ABIERTA) {
            ArqueoResumen arqueo = calcularArqueo(
                    sesion.getCuenta().getId(),
                    sesion.getFechaApertura(),
                    LocalDateTime.now(),
                    sesion.getMontoApertura());
            r.setTotalIngresos(arqueo.totalIngresos);
            r.setTotalEgresos(arqueo.totalEgresos);
            r.setMontoEsperado(arqueo.montoEsperado);
            r.setCantidadMovimientos(arqueo.cantidadMovimientos);
            r.setMontoContado(null);
            r.setDiferencia(null);
        } else {
            LocalDateTime hasta = sesion.getFechaCierre() != null
                    ? sesion.getFechaCierre()
                    : LocalDateTime.now();
            ArqueoResumen arqueo = calcularArqueo(
                    sesion.getCuenta().getId(),
                    sesion.getFechaApertura(),
                    hasta,
                    sesion.getMontoApertura());
            r.setTotalIngresos(sesion.getTotalIngresos() != null ? sesion.getTotalIngresos() : arqueo.totalIngresos());
            r.setTotalEgresos(sesion.getTotalEgresos() != null ? sesion.getTotalEgresos() : arqueo.totalEgresos());
            r.setMontoEsperado(sesion.getMontoEsperadoCierre() != null
                    ? sesion.getMontoEsperadoCierre()
                    : arqueo.montoEsperado());
            r.setMontoContado(sesion.getMontoContadoCierre());
            r.setDiferencia(sesion.getDiferencia());
            r.setCantidadMovimientos(arqueo.cantidadMovimientos());
        }
        return r;
    }

    private CajaMovimientoResponse mapMovimiento(MovimientoFinanciero movimiento) {
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

    private ArqueoResumen calcularArqueo(
            Long cuentaId,
            LocalDateTime desde,
            LocalDateTime hasta,
            BigDecimal montoApertura) {
        List<MovimientoFinanciero> movimientos =
                movimientoFinancieroRepository.findByCuentaIdAndCreatedAtBetween(cuentaId, desde, hasta);

        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal egresos = BigDecimal.ZERO;
        for (MovimientoFinanciero m : movimientos) {
            switch (m.getTipo()) {
                case INGRESO, TRANSFERENCIA_RECIBIDA -> ingresos = ingresos.add(m.getMonto());
                case EGRESO, TRANSFERENCIA_ENVIADA -> egresos = egresos.add(m.getMonto());
                default -> { /* AJUSTE / transferencias no aplican al arqueo de efectivo del turno */ }
            }
        }
        BigDecimal esperado = montoApertura.add(ingresos).subtract(egresos);
        return new ArqueoResumen(ingresos, egresos, esperado, movimientos.size());
    }

    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            return auth.getName();
        }
        return null;
    }

    private record ArqueoResumen(
            BigDecimal totalIngresos,
            BigDecimal totalEgresos,
            BigDecimal montoEsperado,
            int cantidadMovimientos) {}
}
