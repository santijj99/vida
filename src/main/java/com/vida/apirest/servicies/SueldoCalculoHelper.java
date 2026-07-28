package com.vida.apirest.servicies;

import com.vida.apirest.model.sueldo.PeriodoSueldo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Cálculos de sueldo/prorrateo (extraídos para testear sin Spring).
 */
public final class SueldoCalculoHelper {

    private SueldoCalculoHelper() {
    }

    /**
     * Prorratea el sueldo fijo al rango [desde, hasta] según el periodo base del empleado.
     * Para base MES/PERSONALIZADO, suma día a día (fijo / días del mes de ese día)
     * para no distorsionar rangos que cruzan meses.
     * Para base DIA, multiplica el fijo por la cantidad de días laborables del empleado
     * dentro del rango (ISO 1=lun … 7=dom). Si no se indican, cuenta todos los días.
     */
    public static BigDecimal prorratearSueldoFijo(
            BigDecimal sueldoFijo,
            PeriodoSueldo periodoBase,
            LocalDate desde,
            LocalDate hasta
    ) {
        return prorratearSueldoFijo(sueldoFijo, periodoBase, desde, hasta, null);
    }

    public static BigDecimal prorratearSueldoFijo(
            BigDecimal sueldoFijo,
            PeriodoSueldo periodoBase,
            LocalDate desde,
            LocalDate hasta,
            Collection<Integer> diasLaborablesIso
    ) {
        BigDecimal fijo = sueldoFijo != null ? sueldoFijo : BigDecimal.ZERO;
        if (fijo.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            return BigDecimal.ZERO;
        }
        PeriodoSueldo base = periodoBase != null ? periodoBase : PeriodoSueldo.MES;
        return switch (base) {
            case DIA -> {
                Set<DayOfWeek> dias = resolverDiasLaborables(diasLaborablesIso);
                long n = contarDiasLaborables(desde, hasta, dias);
                yield fijo.multiply(BigDecimal.valueOf(n)).setScale(2, RoundingMode.HALF_UP);
            }
            case SEMANA -> {
                long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
                yield fijo.multiply(BigDecimal.valueOf(dias))
                        .divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);
            }
            case QUINCENA -> {
                long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
                yield fijo.multiply(BigDecimal.valueOf(dias))
                        .divide(BigDecimal.valueOf(15), 2, RoundingMode.HALF_UP);
            }
            case MES, PERSONALIZADO -> prorratearPorDiasDelMes(fijo, desde, hasta);
        };
    }

    /**
     * ISO-8601: 1=lunes … 7=domingo. Vacío/null → todos los días (compatibilidad).
     */
    public static Set<DayOfWeek> resolverDiasLaborables(Collection<Integer> diasLaborablesIso) {
        if (diasLaborablesIso == null || diasLaborablesIso.isEmpty()) {
            return EnumSet.allOf(DayOfWeek.class);
        }
        EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
        for (Integer v : diasLaborablesIso) {
            if (v != null && v >= 1 && v <= 7) {
                set.add(DayOfWeek.of(v));
            }
        }
        return set.isEmpty() ? EnumSet.allOf(DayOfWeek.class) : set;
    }

    public static long contarDiasLaborables(LocalDate desde, LocalDate hasta, Set<DayOfWeek> dias) {
        if (desde == null || hasta == null || hasta.isBefore(desde) || dias == null || dias.isEmpty()) {
            return 0;
        }
        long n = 0;
        LocalDate d = desde;
        while (!d.isAfter(hasta)) {
            if (dias.contains(d.getDayOfWeek())) {
                n++;
            }
            d = d.plusDays(1);
        }
        return n;
    }

    private static BigDecimal prorratearPorDiasDelMes(BigDecimal fijo, LocalDate desde, LocalDate hasta) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate d = desde;
        while (!d.isAfter(hasta)) {
            int diasMes = d.lengthOfMonth();
            total = total.add(fijo.divide(BigDecimal.valueOf(diasMes), 6, RoundingMode.HALF_UP));
            d = d.plusDays(1);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal comision(BigDecimal ventas, BigDecimal porcentaje) {
        BigDecimal v = ventas != null ? ventas : BigDecimal.ZERO;
        BigDecimal p = porcentaje != null ? porcentaje : BigDecimal.ZERO;
        return v.multiply(p).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Aplica descuento de días no trabajados sobre el sueldo ya calculado.
     * DIA: (días laborables − descontados) × fijo.
     * Otros: reduce en proporción a los días calendario del rango.
     */
    public static BigDecimal aplicarDiasDescontados(
            BigDecimal sueldoCalculado,
            BigDecimal sueldoFijo,
            PeriodoSueldo periodoBase,
            LocalDate desde,
            LocalDate hasta,
            Collection<Integer> diasLaborablesIso,
            int diasDescontados
    ) {
        if (diasDescontados <= 0) {
            return sueldoCalculado != null ? sueldoCalculado : BigDecimal.ZERO;
        }
        BigDecimal base = sueldoCalculado != null ? sueldoCalculado : BigDecimal.ZERO;
        if (base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        PeriodoSueldo periodo = periodoBase != null ? periodoBase : PeriodoSueldo.MES;
        if (periodo == PeriodoSueldo.DIA) {
            BigDecimal fijo = sueldoFijo != null ? sueldoFijo : BigDecimal.ZERO;
            Set<DayOfWeek> dias = resolverDiasLaborables(diasLaborablesIso);
            long laborables = contarDiasLaborables(desde, hasta, dias);
            long aPagar = Math.max(0, laborables - diasDescontados);
            return fijo.multiply(BigDecimal.valueOf(aPagar)).setScale(2, RoundingMode.HALF_UP);
        }
        long calendario = ChronoUnit.DAYS.between(desde, hasta) + 1;
        if (calendario <= 0) {
            return BigDecimal.ZERO;
        }
        long aPagar = Math.max(0, calendario - diasDescontados);
        return base.multiply(BigDecimal.valueOf(aPagar))
                .divide(BigDecimal.valueOf(calendario), 2, RoundingMode.HALF_UP);
    }
}
