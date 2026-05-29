package com.vida.apirest.servicies;

import com.vida.apirest.dto.venta.CreditoCuotaPreviewResponse;
import com.vida.apirest.dto.venta.CreditoSimulacionResponse;
import com.vida.apirest.model.credito.Cuota;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class CreditoPlanificador {

    public static final String MODO_CUOTAS_IGUALES = "CUOTAS_IGUALES";
    public static final String MODO_ANTICIPO_SUMA_CUOTAS = "ANTICIPO_SUMA_CUOTAS";
    public static final String MODO_PRIMERA_CUOTA_ANTICIPO = "PRIMERA_CUOTA_ANTICIPO";
    public static final String MODO_REDUCE_PRIMERA_CUOTA = "REDUCIR_PRIMERA_CUOTA";

    private CreditoPlanificador() {
    }

    public static class CuotaPlan {
        public int numero;
        public String etiqueta;
        public LocalDateTime fechaVencimiento;
        public BigDecimal monto;
        public BigDecimal saldo;
        public Cuota.EstadoCuota estado;
        public String descripcion;
        public boolean anticipo;
        public boolean pagadaAlCrear;
    }

    public static class ResultadoPlan {
        public BigDecimal montoSubtotal;
        public BigDecimal montoInteres;
        public BigDecimal montoTotal;
        public BigDecimal montoAnticipo;
        public BigDecimal montoFinanciado;
        public String modoDistribucion;
        public String resumen;
        public List<CuotaPlan> cuotas = new ArrayList<>();
    }

    public static ResultadoPlan planificar(
            BigDecimal subtotal,
            int plazoMeses,
            BigDecimal tasaInteresPct,
            BigDecimal montoAnticipo,
            String modoDistribucion,
            LocalDateTime fechaInicio
    ) {
        if (plazoMeses <= 0) {
            throw new RuntimeException("El plazo debe ser mayor a cero");
        }
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto total debe ser mayor a cero");
        }

        String modo = normalizarModo(modoDistribucion);
        BigDecimal tasa = tasaInteresPct != null ? tasaInteresPct : BigDecimal.ZERO;
        BigDecimal interes = subtotal.multiply(tasa).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalConInteres = subtotal.add(interes);

        BigDecimal anticipo = montoAnticipo != null ? montoAnticipo : BigDecimal.ZERO;
        if (anticipo.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("El anticipo no puede ser negativo");
        }
        if (anticipo.compareTo(totalConInteres) > 0) {
            throw new RuntimeException("El anticipo no puede superar el total de la venta");
        }

        LocalDateTime baseFecha = fechaInicio != null ? fechaInicio : LocalDateTime.now();

        ResultadoPlan resultado = new ResultadoPlan();
        resultado.montoSubtotal = subtotal;
        resultado.montoInteres = interes;
        resultado.montoTotal = totalConInteres;
        resultado.montoAnticipo = anticipo;
        resultado.modoDistribucion = modo;

        switch (modo) {
            case MODO_PRIMERA_CUOTA_ANTICIPO -> planificarPrimeraCuotaAnticipo(resultado, totalConInteres, anticipo, plazoMeses, baseFecha);
            case MODO_REDUCE_PRIMERA_CUOTA -> planificarReducePrimeraCuota(resultado, totalConInteres, anticipo, plazoMeses, baseFecha);
            case MODO_ANTICIPO_SUMA_CUOTAS -> planificarAnticipoSumaCuotas(resultado, totalConInteres, anticipo, plazoMeses, baseFecha);
            default -> planificarCuotasIguales(resultado, totalConInteres, anticipo, plazoMeses, baseFecha);
        }

        resultado.montoFinanciado = resultado.cuotas.stream()
                .map(c -> c.saldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        resultado.resumen = construirResumen(resultado);
        return resultado;
    }

    public static CreditoSimulacionResponse toSimulacionResponse(ResultadoPlan plan, int plazoMeses, BigDecimal tasaInteres) {
        CreditoSimulacionResponse response = new CreditoSimulacionResponse();
        response.setMontoSubtotal(plan.montoSubtotal);
        response.setMontoInteres(plan.montoInteres);
        response.setMontoTotal(plan.montoTotal);
        response.setMontoAnticipo(plan.montoAnticipo);
        response.setMontoFinanciado(plan.montoFinanciado);
        response.setPlazoMeses(plazoMeses);
        response.setTasaInteres(tasaInteres);
        response.setModoDistribucion(plan.modoDistribucion);
        response.setResumen(plan.resumen);
        response.setCuotas(plan.cuotas.stream().map(CreditoPlanificador::toPreview).toList());
        return response;
    }

    public static CreditoCuotaPreviewResponse toPreview(CuotaPlan cuota) {
        return new CreditoCuotaPreviewResponse(
                cuota.numero,
                cuota.etiqueta,
                cuota.fechaVencimiento,
                cuota.monto,
                cuota.saldo,
                cuota.estado.name(),
                cuota.descripcion,
                cuota.anticipo,
                cuota.pagadaAlCrear
        );
    }

    public static List<Cuota> materializarCuotas(
            com.vida.apirest.model.credito.Credito credito,
            ResultadoPlan plan
    ) {
        List<Cuota> cuotas = new ArrayList<>();
        for (CuotaPlan item : plan.cuotas) {
            Cuota cuota = new Cuota();
            cuota.setCredito(credito);
            cuota.setNumero(item.etiqueta);
            cuota.setFechaVencimiento(item.fechaVencimiento);
            cuota.setMonto(item.monto);
            cuota.setSaldo(item.saldo);
            cuota.setEstado(item.estado);
            cuota.setDescripcion(item.descripcion);
            cuotas.add(cuota);
        }
        return cuotas;
    }

    private static void planificarCuotasIguales(
            ResultadoPlan resultado,
            BigDecimal totalConInteres,
            BigDecimal anticipo,
            int plazoMeses,
            LocalDateTime baseFecha
    ) {
        BigDecimal aFinanciar = totalConInteres.subtract(anticipo);
        if (aFinanciar.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Con este anticipo no queda saldo para financiar en cuotas");
        }
        resultado.cuotas.addAll(dividirEnCuotas(aFinanciar, plazoMeses, plazoMeses, 1, baseFecha, false, false));
    }

    private static void planificarAnticipoSumaCuotas(
            ResultadoPlan resultado,
            BigDecimal totalConInteres,
            BigDecimal anticipo,
            int plazoMeses,
            LocalDateTime baseFecha
    ) {
        planificarCuotasIguales(resultado, totalConInteres, anticipo, plazoMeses, baseFecha);
        resultado.modoDistribucion = MODO_ANTICIPO_SUMA_CUOTAS;
    }

    private static void planificarPrimeraCuotaAnticipo(
            ResultadoPlan resultado,
            BigDecimal totalConInteres,
            BigDecimal anticipo,
            int plazoMeses,
            LocalDateTime baseFecha
    ) {
        if (plazoMeses < 2) {
            throw new RuntimeException("Para marcar la primera cuota como anticipo se requieren al menos 2 cuotas");
        }
        if (anticipo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Indicá un monto de anticipo para la primera cuota");
        }

        CuotaPlan primera = new CuotaPlan();
        primera.numero = 1;
        primera.etiqueta = "CU-1/" + plazoMeses;
        primera.fechaVencimiento = baseFecha.plusMonths(1);
        primera.monto = anticipo;
        primera.saldo = BigDecimal.ZERO;
        primera.estado = Cuota.EstadoCuota.PAGADA;
        primera.descripcion = "Cuota 1 (anticipo pagado al crear la venta)";
        primera.anticipo = true;
        primera.pagadaAlCrear = true;
        resultado.cuotas.add(primera);

        BigDecimal resto = totalConInteres.subtract(anticipo);
        if (resto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El anticipo cubre el total; no hay cuotas restantes");
        }
        resultado.cuotas.addAll(dividirEnCuotas(resto, plazoMeses - 1, plazoMeses, 2, baseFecha, false, false));
    }

    private static void planificarReducePrimeraCuota(
            ResultadoPlan resultado,
            BigDecimal totalConInteres,
            BigDecimal anticipo,
            int plazoMeses,
            LocalDateTime baseFecha
    ) {
        BigDecimal cuotaBase = totalConInteres.divide(BigDecimal.valueOf(plazoMeses), 2, RoundingMode.HALF_UP);
        BigDecimal montoPrimera = cuotaBase.subtract(anticipo);
        if (montoPrimera.compareTo(BigDecimal.ZERO) < 0) {
            montoPrimera = BigDecimal.ZERO;
        }

        CuotaPlan primera = new CuotaPlan();
        primera.numero = 1;
        primera.etiqueta = "CU-1/" + plazoMeses;
        primera.fechaVencimiento = baseFecha.plusMonths(1);
        primera.monto = cuotaBase;
        primera.saldo = montoPrimera;
        primera.estado = montoPrimera.compareTo(BigDecimal.ZERO) == 0
                ? Cuota.EstadoCuota.PAGADA
                : Cuota.EstadoCuota.PENDIENTE;
        primera.descripcion = anticipo.compareTo(BigDecimal.ZERO) > 0
                ? "Cuota 1 (anticipo $" + anticipo + " aplicado)"
                : "Cuota 1 de " + plazoMeses;
        primera.anticipo = anticipo.compareTo(BigDecimal.ZERO) > 0;
        primera.pagadaAlCrear = montoPrimera.compareTo(BigDecimal.ZERO) == 0;
        resultado.cuotas.add(primera);

        BigDecimal resto = totalConInteres.subtract(anticipo).subtract(montoPrimera);
        if (plazoMeses > 1) {
            if (resto.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("No hay saldo para las cuotas restantes");
            }
            resultado.cuotas.addAll(dividirEnCuotas(resto, plazoMeses - 1, plazoMeses, 2, baseFecha, false, false));
        }
    }

    private static List<CuotaPlan> dividirEnCuotas(
            BigDecimal importe,
            int cantidadCuotas,
            int totalPlazoEtiqueta,
            int numeroInicial,
            LocalDateTime baseFecha,
            boolean anticipo,
            boolean pagadaAlCrear
    ) {
        List<CuotaPlan> cuotas = new ArrayList<>();
        BigDecimal cuotaBase = importe.divide(BigDecimal.valueOf(cantidadCuotas), 2, RoundingMode.HALF_UP);

        for (int i = 0; i < cantidadCuotas; i++) {
            int numero = numeroInicial + i;
            boolean ultima = i == cantidadCuotas - 1;
            BigDecimal monto = ultima
                    ? importe.subtract(cuotaBase.multiply(BigDecimal.valueOf(cantidadCuotas - 1)))
                    : cuotaBase;

            CuotaPlan cuota = new CuotaPlan();
            cuota.numero = numero;
            cuota.etiqueta = "CU-" + numero + "/" + totalPlazoEtiqueta;
            cuota.fechaVencimiento = baseFecha.plusMonths(numero);
            cuota.monto = monto;
            cuota.saldo = pagadaAlCrear ? BigDecimal.ZERO : monto;
            cuota.estado = pagadaAlCrear ? Cuota.EstadoCuota.PAGADA : Cuota.EstadoCuota.PENDIENTE;
            cuota.descripcion = "Cuota " + numero;
            cuota.anticipo = anticipo;
            cuota.pagadaAlCrear = pagadaAlCrear;
            cuotas.add(cuota);
        }
        return cuotas;
    }

    private static String normalizarModo(String modo) {
        if (modo == null || modo.isBlank()) {
            return MODO_CUOTAS_IGUALES;
        }
        return switch (modo.toUpperCase()) {
            case MODO_ANTICIPO_SUMA_CUOTAS -> MODO_ANTICIPO_SUMA_CUOTAS;
            case MODO_PRIMERA_CUOTA_ANTICIPO -> MODO_PRIMERA_CUOTA_ANTICIPO;
            case MODO_REDUCE_PRIMERA_CUOTA -> MODO_REDUCE_PRIMERA_CUOTA;
            default -> MODO_CUOTAS_IGUALES;
        };
    }

    private static String construirResumen(ResultadoPlan plan) {
        long pagadas = plan.cuotas.stream().filter(c -> c.estado == Cuota.EstadoCuota.PAGADA).count();
        return String.format(
                "Total $%s (interés $%s). Anticipo $%s. A financiar $%s en %d cuota(s), %d pagada(s) al crear.",
                plan.montoTotal,
                plan.montoInteres,
                plan.montoAnticipo,
                plan.montoFinanciado,
                plan.cuotas.size(),
                pagadas
        );
    }
}
