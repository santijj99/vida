package com.vida.apirest.servicies;

import com.vida.apirest.model.credito.CreditoConfigEmpresa;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class CreditoVencimientoCalculoService {

    /**
     * Calcula la fecha de vencimiento de una cuota según la configuración de la empresa.
     *
     * @param fechaBase     fecha de referencia (primer vencimiento o alta)
     * @param numeroCuota   1-based
     * @param config        configuración de créditos
     */
    public LocalDateTime calcularFechaVencimiento(
            LocalDateTime fechaBase,
            int numeroCuota,
            CreditoConfigEmpresa config
    ) {
        LocalDate base = fechaBase != null ? fechaBase.toLocalDate() : LocalDate.now();
        // base = 1er vencimiento − 1 mes → cuota N = base + N meses
        YearMonth mes = YearMonth.from(base).plusMonths(numeroCuota);
        CreditoConfigEmpresa.ModoDiaVencimiento modo = config != null && config.getModoDiaVencimiento() != null
                ? config.getModoDiaVencimiento()
                : CreditoConfigEmpresa.ModoDiaVencimiento.DIA_10;

        int dia = switch (modo) {
            case DIA_1 -> 1;
            case DIA_5 -> 5;
            case DIA_10 -> 10;
            case DIA_15 -> 15;
            case DIA_20 -> 20;
            case RANGO_1_10 -> Math.min(10, Math.max(1, base.getDayOfMonth()));
            case RANGO_1_15 -> Math.min(15, Math.max(1, base.getDayOfMonth()));
            case ULTIMO_MES -> mes.lengthOfMonth();
            case DIA_PERSONALIZADO -> base.getDayOfMonth();
        };

        int maxDia = mes.lengthOfMonth();
        LocalDate fecha = mes.atDay(Math.min(dia, maxDia));
        return fecha.atStartOfDay();
    }
}
