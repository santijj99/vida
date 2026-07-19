package com.vida.apirest.servicies;

import com.vida.apirest.exception.ResourceNotFoundException;
import com.vida.apirest.model.finanzas.CuentaFinanciera;
import com.vida.apirest.model.tesoreria.MovimientoFinanciero;
import com.vida.apirest.repositories.FinanzasCuentaFinancieraRepository;
import com.vida.apirest.repositories.MovimientoFinancieroRepository;
import com.vida.apirest.utils.EntityLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CajaMovimientoService {

    private final FinanzasCuentaFinancieraRepository cuentaRepository;
    private final MovimientoFinancieroRepository movimientoFinancieroRepository;

    @Transactional
    public MovimientoFinanciero registrarIngreso(
            CuentaFinanciera cuenta,
            BigDecimal monto,
            String descripcion,
            String referencia
    ) {
        return persistir(cuenta, MovimientoFinanciero.TipoMovimiento.INGRESO, monto, descripcion, referencia);
    }

    @Transactional
    public MovimientoFinanciero registrarIngreso(Long cuentaFinancieraId, BigDecimal monto, String descripcion) {
        CuentaFinanciera cuenta = resolverCuentaIngreso(cuentaFinancieraId);
        if (cuenta == null) {
            return null;
        }
        return registrarIngreso(cuenta, monto, descripcion, null);
    }

    @Transactional
    public void registrarEgreso(
            CuentaFinanciera cuenta,
            BigDecimal monto,
            String descripcion,
            String referencia
    ) {
        persistir(cuenta, MovimientoFinanciero.TipoMovimiento.EGRESO, monto, descripcion, referencia);
    }

    @Transactional
    public void registrarEgreso(Long cuentaFinancieraId, BigDecimal monto, String descripcion) {
        if (cuentaFinancieraId == null || monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        CuentaFinanciera cuenta = EntityLookup.require(
                cuentaRepository.findById(cuentaFinancieraId),
                "Cuenta financiera no encontrada");
        persistir(cuenta, MovimientoFinanciero.TipoMovimiento.EGRESO, monto, descripcion, null);
    }

    private CuentaFinanciera resolverCuentaIngreso(Long cuentaFinancieraId) {
        if (cuentaFinancieraId != null) {
            return EntityLookup.require(
                    cuentaRepository.findById(cuentaFinancieraId),
                    "Cuenta financiera no encontrada");
        }
        return cuentaRepository.findFirstByTipoAndActivoTrue(CuentaFinanciera.TipoCuenta.CAJA).orElse(null);
    }

    private MovimientoFinanciero persistir(
            CuentaFinanciera cuenta,
            MovimientoFinanciero.TipoMovimiento tipo,
            BigDecimal monto,
            String descripcion,
            String referencia
    ) {
        if (cuenta == null) {
            throw new ResourceNotFoundException("Cuenta financiera no encontrada");
        }
        BigDecimal saldoAnterior = saldoActual(cuenta);
        BigDecimal saldoNuevo = tipo == MovimientoFinanciero.TipoMovimiento.INGRESO
                ? saldoAnterior.add(monto)
                : saldoAnterior.subtract(monto).max(BigDecimal.ZERO);

        cuenta.setSaldoActual(saldoNuevo);
        cuentaRepository.save(cuenta);

        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setCuenta(cuenta);
        movimiento.setNumero(generarNumero());
        movimiento.setTipo(tipo);
        movimiento.setMonto(monto);
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoNuevo(saldoNuevo);
        movimiento.setDescripcion(descripcion);
        movimiento.setReferencia(referencia);
        movimiento.setResponsable("sistema");
        return movimientoFinancieroRepository.save(movimiento);
    }

    private BigDecimal saldoActual(CuentaFinanciera cuenta) {
        return cuenta.getSaldoActual() != null ? cuenta.getSaldoActual() : BigDecimal.ZERO;
    }

    private String generarNumero() {
        return "MV-" + UUID.randomUUID().toString().replace("-", "");
    }
}
