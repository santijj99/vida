package com.vida.apirest.servicies;

import com.vida.apirest.exception.BadRequestException;
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
    public MovimientoFinanciero registrarEgreso(
            CuentaFinanciera cuenta,
            BigDecimal monto,
            String descripcion,
            String referencia
    ) {
        return persistir(cuenta, MovimientoFinanciero.TipoMovimiento.EGRESO, monto, descripcion, referencia);
    }

    @Transactional
    public MovimientoFinanciero registrarEgreso(Long cuentaFinancieraId, BigDecimal monto, String descripcion) {
        if (cuentaFinancieraId == null || monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        CuentaFinanciera cuenta = EntityLookup.require(
                cuentaRepository.findById(cuentaFinancieraId),
                "Cuenta financiera no encontrada");
        return persistir(cuenta, MovimientoFinanciero.TipoMovimiento.EGRESO, monto, descripcion, null);
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
        if (cuenta == null || cuenta.getId() == null) {
            throw new ResourceNotFoundException("Cuenta financiera no encontrada");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El monto del movimiento debe ser mayor a cero");
        }
        // Releer con lock: no confiar en el saldo en memoria (puede estar stale entre TX).
        CuentaFinanciera locked = cuentaRepository.findByIdForUpdate(cuenta.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta financiera no encontrada"));

        BigDecimal saldoAnterior = saldoActual(locked);
        BigDecimal saldoNuevo;
        if (tipo == MovimientoFinanciero.TipoMovimiento.INGRESO) {
            saldoNuevo = saldoAnterior.add(monto);
        } else {
            if (saldoAnterior.compareTo(monto) < 0) {
                throw new BadRequestException(
                        "Saldo insuficiente en la cuenta \"" + locked.getNombre() + "\". "
                                + "Disponible: " + saldoAnterior + ", requerido: " + monto);
            }
            saldoNuevo = saldoAnterior.subtract(monto);
        }

        locked.setSaldoActual(saldoNuevo);
        cuentaRepository.save(locked);

        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setCuenta(locked);
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
