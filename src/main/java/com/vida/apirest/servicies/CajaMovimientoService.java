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

  /**
   * Registra un ajuste de caja (faltante o sobrante de efectivo).
   * El monto se guarda siempre positivo; el sentido se refleja en saldoNuevo vs saldoAnterior.
   */
    @Transactional
    public MovimientoFinanciero registrarAjuste(
            Long cuentaId,
            String sentido,
            BigDecimal monto,
            String motivo,
            String responsable
    ) {
        if (cuentaId == null) {
            throw new BadRequestException("Debe indicar la caja");
        }
        if (sentido == null || sentido.isBlank()) {
            throw new BadRequestException("Indicá el sentido del ajuste (FALTANTE o SOBRANTE)");
        }
        String sentidoNorm = sentido.trim().toUpperCase();
        if (!sentidoNorm.equals("FALTANTE") && !sentidoNorm.equals("SOBRANTE")) {
            throw new BadRequestException("Sentido inválido. Use FALTANTE o SOBRANTE");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El monto del ajuste debe ser mayor a cero");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new BadRequestException("Indicá el motivo del ajuste");
        }

        CuentaFinanciera cuenta = EntityLookup.require(
                cuentaRepository.findById(cuentaId),
                "Cuenta financiera no encontrada");
        if (cuenta.getTipo() != CuentaFinanciera.TipoCuenta.CAJA) {
            throw new BadRequestException("Solo se pueden registrar ajustes en cuentas tipo CAJA");
        }
        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            throw new BadRequestException("La caja no está activa");
        }

        CuentaFinanciera locked = cuentaRepository.findByIdForUpdate(cuenta.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta financiera no encontrada"));

        BigDecimal saldoAnterior = saldoActual(locked);
        BigDecimal saldoNuevo;
        if (sentidoNorm.equals("SOBRANTE")) {
            saldoNuevo = saldoAnterior.add(monto);
        } else {
            if (saldoAnterior.compareTo(monto) < 0) {
                throw new BadRequestException(
                        "Saldo insuficiente en la caja \"" + locked.getNombre() + "\". "
                                + "Disponible: " + saldoAnterior + ", requerido: " + monto);
            }
            saldoNuevo = saldoAnterior.subtract(monto);
        }

        locked.setSaldoActual(saldoNuevo);
        cuentaRepository.save(locked);

        String etiqueta = sentidoNorm.equals("SOBRANTE")
                ? "Entrada de efectivo (ajuste)"
                : "Salida de efectivo (ajuste)";
        String descripcion = etiqueta + ": " + motivo.trim();
        String user = (responsable == null || responsable.isBlank()) ? "sistema" : responsable.trim();

        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setCuenta(locked);
        movimiento.setNumero(generarNumero());
        movimiento.setTipo(MovimientoFinanciero.TipoMovimiento.AJUSTE);
        movimiento.setMonto(monto);
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoNuevo(saldoNuevo);
        movimiento.setDescripcion(descripcion);
        movimiento.setReferencia("AJ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        movimiento.setResponsable(user);
        return movimientoFinancieroRepository.save(movimiento);
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
        return persistir(cuenta, tipo, monto, descripcion, referencia, "sistema");
    }

    private MovimientoFinanciero persistir(
            CuentaFinanciera cuenta,
            MovimientoFinanciero.TipoMovimiento tipo,
            BigDecimal monto,
            String descripcion,
            String referencia,
            String responsable
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
        if (tipo == MovimientoFinanciero.TipoMovimiento.INGRESO
                || tipo == MovimientoFinanciero.TipoMovimiento.TRANSFERENCIA_RECIBIDA) {
            saldoNuevo = saldoAnterior.add(monto);
        } else if (tipo == MovimientoFinanciero.TipoMovimiento.EGRESO
                || tipo == MovimientoFinanciero.TipoMovimiento.TRANSFERENCIA_ENVIADA) {
            if (saldoAnterior.compareTo(monto) < 0) {
                throw new BadRequestException(
                        "Saldo insuficiente en la cuenta \"" + locked.getNombre() + "\". "
                                + "Disponible: " + saldoAnterior + ", requerido: " + monto);
            }
            saldoNuevo = saldoAnterior.subtract(monto);
        } else {
            // AJUSTE: se trata como set absoluto vía monto positivo/negativo no soportado aquí
            throw new BadRequestException("Tipo de movimiento no soportado: " + tipo);
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
        movimiento.setResponsable(responsable == null || responsable.isBlank() ? "sistema" : responsable);
        return movimientoFinancieroRepository.save(movimiento);
    }

    /**
     * Transfiere fondos entre dos cuentas (caja↔caja, caja↔banco, banco↔banco).
     * Genera TRANSFERENCIA_ENVIADA en origen y TRANSFERENCIA_RECIBIDA en destino.
     */
    @Transactional
    public TransferenciaResult transferir(
            Long cuentaOrigenId,
            Long cuentaDestinoId,
            BigDecimal monto,
            String descripcion,
            String responsable
    ) {
        if (cuentaOrigenId == null || cuentaDestinoId == null) {
            throw new BadRequestException("Indicá cuenta origen y destino");
        }
        if (cuentaOrigenId.equals(cuentaDestinoId)) {
            throw new BadRequestException("La cuenta origen y destino deben ser distintas");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El monto a transferir debe ser mayor a cero");
        }

        // Orden de locks por id para evitar deadlocks.
        Long firstId = Math.min(cuentaOrigenId, cuentaDestinoId);
        Long secondId = Math.max(cuentaOrigenId, cuentaDestinoId);
        CuentaFinanciera first = cuentaRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta financiera no encontrada: " + firstId));
        CuentaFinanciera second = cuentaRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta financiera no encontrada: " + secondId));

        CuentaFinanciera origen = first.getId().equals(cuentaOrigenId) ? first : second;
        CuentaFinanciera destino = first.getId().equals(cuentaDestinoId) ? first : second;

        if (!Boolean.TRUE.equals(origen.getActivo()) || !Boolean.TRUE.equals(destino.getActivo())) {
            throw new BadRequestException("Ambas cuentas deben estar activas para transferir");
        }

        String ref = "TR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String desc = (descripcion == null || descripcion.isBlank())
                ? "Transferencia " + origen.getNombre() + " → " + destino.getNombre()
                : descripcion.trim();
        String user = (responsable == null || responsable.isBlank()) ? "sistema" : responsable.trim();

        MovimientoFinanciero enviado = persistirLocked(
                origen,
                MovimientoFinanciero.TipoMovimiento.TRANSFERENCIA_ENVIADA,
                monto,
                desc + " (enviada a " + destino.getNombre() + ")",
                ref,
                user
        );
        MovimientoFinanciero recibido = persistirLocked(
                destino,
                MovimientoFinanciero.TipoMovimiento.TRANSFERENCIA_RECIBIDA,
                monto,
                desc + " (recibida de " + origen.getNombre() + ")",
                ref,
                user
        );

        return new TransferenciaResult(enviado, recibido, origen, destino, ref, desc);
    }

    /** Variante cuando la cuenta ya está bloqueada en la TX actual. */
    private MovimientoFinanciero persistirLocked(
            CuentaFinanciera locked,
            MovimientoFinanciero.TipoMovimiento tipo,
            BigDecimal monto,
            String descripcion,
            String referencia,
            String responsable
    ) {
        BigDecimal saldoAnterior = saldoActual(locked);
        BigDecimal saldoNuevo;
        if (tipo == MovimientoFinanciero.TipoMovimiento.INGRESO
                || tipo == MovimientoFinanciero.TipoMovimiento.TRANSFERENCIA_RECIBIDA) {
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
        movimiento.setResponsable(responsable);
        return movimientoFinancieroRepository.save(movimiento);
    }

    public record TransferenciaResult(
            MovimientoFinanciero enviado,
            MovimientoFinanciero recibido,
            CuentaFinanciera origen,
            CuentaFinanciera destino,
            String referencia,
            String descripcion
    ) {
    }

    private BigDecimal saldoActual(CuentaFinanciera cuenta) {
        return cuenta.getSaldoActual() != null ? cuenta.getSaldoActual() : BigDecimal.ZERO;
    }

    private String generarNumero() {
        return "MV-" + UUID.randomUUID().toString().replace("-", "");
    }
}
