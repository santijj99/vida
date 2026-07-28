package com.vida.apirest.servicies;

import com.vida.apirest.model.sueldo.PeriodoSueldo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SueldoCalculoHelperTest {

    @Test
    void prorrateaMesDentroDelMismoMes() {
        // Sueldo 310_000 en mes de 31 días → 10 días = 100_000
        BigDecimal r = SueldoCalculoHelper.prorratearSueldoFijo(
                new BigDecimal("310000"),
                PeriodoSueldo.MES,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 10));
        assertEquals(0, new BigDecimal("100000.00").compareTo(r));
    }

    @Test
    void prorrateaMesCruzandoMeses() {
        // Ene 30-31 (2/31 de 310000) + Feb 1-2 (2/28 de 310000) en 2026 (no bisiesto)
        BigDecimal fijo = new BigDecimal("310000");
        BigDecimal r = SueldoCalculoHelper.prorratearSueldoFijo(
                fijo,
                PeriodoSueldo.MES,
                LocalDate.of(2026, 1, 30),
                LocalDate.of(2026, 2, 2));
        BigDecimal esperado = fijo.divide(new BigDecimal("31"), 6, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("2"))
                .add(fijo.divide(new BigDecimal("28"), 6, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("2")))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(0, esperado.compareTo(r));
        // No debe usar solo lengthOfMonth(enero) para los 4 días
        BigDecimal formulaViejaIncorrecta = fijo.multiply(new BigDecimal("4"))
                .divide(new BigDecimal("31"), 2, java.math.RoundingMode.HALF_UP);
        assertTrue(r.compareTo(formulaViejaIncorrecta) != 0);
    }

    @Test
    void comisionRedondeaHalfUp() {
        BigDecimal c = SueldoCalculoHelper.comision(new BigDecimal("1000.00"), new BigDecimal("12.5"));
        assertEquals(0, new BigDecimal("125.00").compareTo(c));
    }

    @Test
    void prorrateaDiaSoloDiasLaborables() {
        // Julio 2026: 1–31. Lun–Vie = 23 días (no 31).
        BigDecimal diario = new BigDecimal("10000");
        BigDecimal r = SueldoCalculoHelper.prorratearSueldoFijo(
                diario,
                PeriodoSueldo.DIA,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                java.util.List.of(1, 2, 3, 4, 5));
        assertEquals(0, new BigDecimal("230000.00").compareTo(r));
    }

    @Test
    void prorrateaDiaSinListaCuentaTodosLosDias() {
        BigDecimal diario = new BigDecimal("1000");
        BigDecimal r = SueldoCalculoHelper.prorratearSueldoFijo(
                diario,
                PeriodoSueldo.DIA,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                null);
        assertEquals(0, new BigDecimal("31000.00").compareTo(r));
    }

    @Test
    void descuentaDiasEnBaseDiaria() {
        BigDecimal diario = new BigDecimal("10000");
        BigDecimal bruto = SueldoCalculoHelper.prorratearSueldoFijo(
                diario,
                PeriodoSueldo.DIA,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                java.util.List.of(1, 2, 3, 4, 5));
        // 23 laborables − 2 faltas = 21
        BigDecimal r = SueldoCalculoHelper.aplicarDiasDescontados(
                bruto,
                diario,
                PeriodoSueldo.DIA,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                java.util.List.of(1, 2, 3, 4, 5),
                2);
        assertEquals(0, new BigDecimal("210000.00").compareTo(r));
    }

    @Test
    void prorrateaCeroSiFijoNegativoOCero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(
                SueldoCalculoHelper.prorratearSueldoFijo(
                        BigDecimal.ZERO, PeriodoSueldo.MES,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10))));
    }
}
