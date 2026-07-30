package com.vida.apirest.servicies;

import com.vida.apirest.dto.sueldo.EmpleadoSueldoConfigRequest;
import com.vida.apirest.dto.sueldo.EmpleadoSueldoConfigResponse;
import com.vida.apirest.dto.sueldo.LiquidacionSueldoAnularPagoRequest;
import com.vida.apirest.dto.sueldo.LiquidacionSueldoCreateRequest;
import com.vida.apirest.dto.sueldo.LiquidacionSueldoItemDiasDescontadosRequest;
import com.vida.apirest.dto.sueldo.LiquidacionSueldoPagoRequest;
import com.vida.apirest.dto.sueldo.LiquidacionSueldoResponse;
import com.vida.apirest.exception.BadRequestException;
import com.vida.apirest.exception.ForbiddenException;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.auth.Role;
import com.vida.apirest.model.finanzas.CuentaFinanciera;
import com.vida.apirest.model.persona.Empleado;
import com.vida.apirest.model.sueldo.EmpleadoSueldoConfig;
import com.vida.apirest.model.sueldo.LiquidacionSueldo;
import com.vida.apirest.model.sueldo.LiquidacionSueldoItem;
import com.vida.apirest.model.sueldo.PeriodoSueldo;
import com.vida.apirest.repositories.EmpleadoRepository;
import com.vida.apirest.repositories.EmpleadoSueldoConfigRepository;
import com.vida.apirest.repositories.FinanzasCuentaFinancieraRepository;
import com.vida.apirest.repositories.LiquidacionSueldoItemRepository;
import com.vida.apirest.repositories.LiquidacionSueldoRepository;
import com.vida.apirest.repositories.RoleRepository;
import com.vida.apirest.repositories.SucursalRepository;
import com.vida.apirest.security.AppUserDetails;
import com.vida.apirest.security.SucursalScopeService;
import com.vida.apirest.utils.DiasLaborablesCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SueldoService {

    private final EmpleadoSueldoConfigRepository configRepository;
    private final LiquidacionSueldoRepository liquidacionRepository;
    private final LiquidacionSueldoItemRepository itemRepository;
    private final EmpleadoRepository empleadoRepository;
    private final SucursalRepository sucursalRepository;
    private final FinanzasCuentaFinancieraRepository cuentaRepository;
    private final RoleRepository roleRepository;
    private final CajaMovimientoService cajaMovimientoService;
    private final SucursalScopeService sucursalScopeService;

    @Transactional(readOnly = true)
    public List<EmpleadoSueldoConfigResponse> listarConfigs() {
        return configRepository.findAllWithEmpleado().stream().map(this::mapConfig).toList();
    }

    @Transactional
    public EmpleadoSueldoConfigResponse upsertConfig(EmpleadoSueldoConfigRequest request) {
        if (request.getEmpleadoId() == null) {
            throw new BadRequestException("empleadoId es obligatorio");
        }
        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new BadRequestException("Empleado no encontrado"));
        if (!Boolean.TRUE.equals(empleado.getActivo())) {
            throw new BadRequestException("El empleado no está activo");
        }

        BigDecimal sueldoFijo = nz(request.getSueldoFijo());
        if (sueldoFijo.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("El sueldo fijo no puede ser negativo");
        }
        BigDecimal pct = nz(request.getPorcentajeComision());
        validarPorcentajeComision(pct);

        EmpleadoSueldoConfig config = configRepository.findByEmpleado_Id(empleado.getId())
                .orElseGet(EmpleadoSueldoConfig::new);
        config.setEmpleado(empleado);
        config.setSueldoFijo(sueldoFijo);
        PeriodoSueldo periodoBase = request.getPeriodoBase() != null ? request.getPeriodoBase() : PeriodoSueldo.MES;
        config.setPeriodoBase(periodoBase);
        if (periodoBase == PeriodoSueldo.DIA) {
            config.setDiasLaborables(DiasLaborablesCodec.encode(
                    DiasLaborablesCodec.normalizeOrDefaultForDia(request.getDiasLaborables())));
        } else if (request.getDiasLaborables() != null) {
            // Se permite guardar la preferencia aunque el período no sea diario.
            config.setDiasLaborables(DiasLaborablesCodec.encode(request.getDiasLaborables()));
        }
        config.setPorcentajeComision(pct);
        config.setActivo(request.getActivo() == null || request.getActivo());
        config.setObservaciones(blankToNull(request.getObservaciones()));
        return mapConfig(configRepository.save(config));
    }

    @Transactional(readOnly = true)
    public List<LiquidacionSueldoResponse> listarLiquidaciones(Long sucursalId) {
        Long filtro = sucursalScopeService.enforceFilter(sucursalId);
        return liquidacionRepository.listar(filtro).stream()
                .map(l -> mapLiquidacion(l, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public LiquidacionSueldoResponse obtenerLiquidacion(Long id) {
        LiquidacionSueldo liq = liquidacionRepository.findByIdWithItems(id)
                .orElseThrow(() -> new BadRequestException("Liquidación no encontrada"));
        assertAccesoLiquidacion(liq);
        return mapLiquidacion(liq, true);
    }

    @Transactional
    public LiquidacionSueldoResponse crearLiquidacion(LiquidacionSueldoCreateRequest request) {
        PeriodoSueldo periodo = request.getPeriodoTipo() != null ? request.getPeriodoTipo() : PeriodoSueldo.MES;
        LocalDate[] rango = resolverRango(periodo, request.getFechaDesde(), request.getFechaHasta());
        LocalDate desde = rango[0];
        LocalDate hasta = rango[1];
        if (hasta.isBefore(desde)) {
            throw new BadRequestException("fechaHasta no puede ser anterior a fechaDesde");
        }
        if (request.getPorcentajeComisionOverride() != null) {
            validarPorcentajeComision(request.getPorcentajeComisionOverride());
        }

        Long sucursalIdReq = request.getSucursalId();
        if (sucursalIdReq == null && !sucursalScopeService.hasGlobalAccess()) {
            sucursalIdReq = sucursalScopeService.enforceFilter(null);
        }

        Sucursal sucursal = null;
        if (sucursalIdReq != null) {
            sucursalScopeService.assertCanUse(sucursalIdReq);
            sucursal = sucursalRepository.findById(sucursalIdReq)
                    .orElseThrow(() -> new BadRequestException("Sucursal no encontrada"));
        }

        List<EmpleadoSueldoConfig> configs = seleccionarConfigs(request.getEmpleadoIds());
        if (configs.isEmpty()) {
            throw new BadRequestException("No hay empleados con configuración de sueldo activa");
        }

        boolean permitirSolapamiento = Boolean.TRUE.equals(request.getPermitirSolapamiento());
        if (!permitirSolapamiento) {
            List<Long> empleadoIdsCheck = configs.stream()
                    .map(c -> c.getEmpleado().getId())
                    .toList();
            List<Long> solapados = liquidacionRepository.findEmpleadoIdsSolapados(
                    desde,
                    hasta,
                    empleadoIdsCheck,
                    LiquidacionSueldo.EstadoLiquidacion.CANCELADA);
            if (!solapados.isEmpty()) {
                String nombres = configs.stream()
                        .filter(c -> solapados.contains(c.getEmpleado().getId()))
                        .map(c -> nombreEmpleado(c.getEmpleado()))
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(
                        "Ya existe liquidación activa solapada (" + desde + " → " + hasta
                                + ") para: " + nombres
                                + " (incluye otras sucursales). Cancelala, elegí otro rango, "
                                + "excluí a esos empleados o marcá \"permitir solapamiento\" "
                                + "si estás seguro de no pagar dos veces sueldo/comisiones.");
            }
        }

        LiquidacionSueldo liq = new LiquidacionSueldo();
        liq.setNumero("LS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        liq.setSucursal(sucursal);
        liq.setPeriodoTipo(periodo);
        liq.setFechaDesde(desde);
        liq.setFechaHasta(hasta);
        liq.setPorcentajeComisionOverride(request.getPorcentajeComisionOverride());
        liq.setObservaciones(blankToNull(request.getObservaciones()));
        liq.setResponsable(usuarioActualNombre());
        liq.setEstado(LiquidacionSueldo.EstadoLiquidacion.CALCULADA);

        boolean prorratear = request.getProrratearSueldo() == null || request.getProrratearSueldo();
        armarItems(liq, configs, prorratear);

        LiquidacionSueldo saved = liquidacionRepository.save(liq);
        return mapLiquidacion(
                liquidacionRepository.findByIdWithItems(saved.getId()).orElse(saved),
                true);
    }

    @Transactional
    public LiquidacionSueldoResponse pagar(Long liquidacionId, LiquidacionSueldoPagoRequest request) {
        if (request.getCuentaId() == null) {
            throw new BadRequestException("cuentaId es obligatorio para pagar");
        }
        liquidacionRepository.findByIdForUpdate(liquidacionId)
                .orElseThrow(() -> new BadRequestException("Liquidación no encontrada"));
        LiquidacionSueldo liq = liquidacionRepository.findByIdWithItems(liquidacionId)
                .orElseThrow(() -> new BadRequestException("Liquidación no encontrada"));
        if (liq.getEstado() == LiquidacionSueldo.EstadoLiquidacion.CANCELADA) {
            throw new BadRequestException("La liquidación está cancelada");
        }
        if (liq.getEstado() == LiquidacionSueldo.EstadoLiquidacion.PAGADA) {
            throw new BadRequestException("La liquidación ya está pagada");
        }
        assertAccesoLiquidacion(liq);

        CuentaFinanciera cuenta = cuentaRepository.findById(request.getCuentaId())
                .orElseThrow(() -> new BadRequestException("Cuenta financiera no encontrada"));
        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            throw new BadRequestException("La cuenta financiera no está activa");
        }
        if (liq.getSucursal() != null
                && !cuenta.getSucursal().getId().equals(liq.getSucursal().getId())) {
            throw new BadRequestException("La cuenta debe pertenecer a la misma sucursal de la liquidación");
        }

        List<LiquidacionSueldoItem> aPagar;
        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            aPagar = liq.getItems().stream()
                    .filter(i -> i.getEstado() == LiquidacionSueldoItem.EstadoItem.PENDIENTE)
                    .toList();
        } else {
            Set<Long> ids = new HashSet<>(request.getItemIds());
            aPagar = liq.getItems().stream()
                    .filter(i -> ids.contains(i.getId()))
                    .toList();
            if (aPagar.size() != ids.size()) {
                throw new BadRequestException("Uno o más ítems no pertenecen a esta liquidación");
            }
            List<Long> yaPagados = aPagar.stream()
                    .filter(i -> i.getEstado() == LiquidacionSueldoItem.EstadoItem.PAGADO)
                    .map(LiquidacionSueldoItem::getId)
                    .toList();
            if (!yaPagados.isEmpty()) {
                throw new BadRequestException("Ítems ya pagados: " + yaPagados);
            }
        }
        if (aPagar.isEmpty()) {
            throw new BadRequestException("No hay ítems pendientes para pagar");
        }

        LocalDateTime ahora = LocalDateTime.now();
        for (LiquidacionSueldoItem item : aPagar) {
            if (item.getEstado() == LiquidacionSueldoItem.EstadoItem.PAGADO) {
                throw new BadRequestException("El ítem " + item.getId() + " ya está pagado");
            }
            if (item.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                var movimiento = cajaMovimientoService.registrarEgreso(
                        cuenta,
                        item.getTotal(),
                        "Sueldo/comisión " + nombreEmpleado(item.getEmpleado()) + " (" + liq.getNumero() + ")",
                        liq.getNumero());
                item.setMovimiento(movimiento);
            }
            item.setEstado(LiquidacionSueldoItem.EstadoItem.PAGADO);
            item.setFechaPago(ahora);
            item.setCuentaPago(cuenta);
        }

        appendObservacion(liq, blankToNull(request.getObservaciones()), "Pago");
        actualizarEstadoSegunItems(liq);

        liquidacionRepository.save(liq);
        return mapLiquidacion(
                liquidacionRepository.findByIdWithItems(liq.getId()).orElse(liq),
                true);
    }

    @Transactional
    public LiquidacionSueldoResponse anularPago(Long liquidacionId, LiquidacionSueldoAnularPagoRequest request) {
        liquidacionRepository.findByIdForUpdate(liquidacionId)
                .orElseThrow(() -> new BadRequestException("Liquidación no encontrada"));
        LiquidacionSueldo liq = liquidacionRepository.findByIdWithItems(liquidacionId)
                .orElseThrow(() -> new BadRequestException("Liquidación no encontrada"));
        if (liq.getEstado() == LiquidacionSueldo.EstadoLiquidacion.CANCELADA) {
            throw new BadRequestException("La liquidación está cancelada");
        }
        assertAccesoLiquidacion(liq);

        List<LiquidacionSueldoItem> aAnular;
        if (request == null || request.getItemIds() == null || request.getItemIds().isEmpty()) {
            aAnular = liq.getItems().stream()
                    .filter(i -> i.getEstado() == LiquidacionSueldoItem.EstadoItem.PAGADO)
                    .toList();
        } else {
            Set<Long> ids = new HashSet<>(request.getItemIds());
            aAnular = liq.getItems().stream()
                    .filter(i -> ids.contains(i.getId()))
                    .toList();
            if (aAnular.size() != ids.size()) {
                throw new BadRequestException("Uno o más ítems no pertenecen a esta liquidación");
            }
            List<Long> noPagados = aAnular.stream()
                    .filter(i -> i.getEstado() != LiquidacionSueldoItem.EstadoItem.PAGADO)
                    .map(LiquidacionSueldoItem::getId)
                    .toList();
            if (!noPagados.isEmpty()) {
                throw new BadRequestException("Ítems no pagados (no se pueden anular): " + noPagados);
            }
        }
        if (aAnular.isEmpty()) {
            throw new BadRequestException("No hay ítems pagados para anular");
        }

        for (LiquidacionSueldoItem item : aAnular) {
            if (item.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                CuentaFinanciera cuenta = item.getCuentaPago();
                if (cuenta == null && item.getMovimiento() != null) {
                    cuenta = item.getMovimiento().getCuenta();
                }
                if (cuenta == null) {
                    throw new BadRequestException(
                            "No se puede anular el ítem " + item.getId()
                                    + ": no hay cuenta de pago asociada");
                }
                cajaMovimientoService.registrarIngreso(
                        cuenta,
                        item.getTotal(),
                        "Anulación sueldo/comisión " + nombreEmpleado(item.getEmpleado())
                                + " (" + liq.getNumero() + ")",
                        liq.getNumero());
            }
            item.setEstado(LiquidacionSueldoItem.EstadoItem.PENDIENTE);
            item.setFechaPago(null);
            item.setCuentaPago(null);
            item.setMovimiento(null);
        }

        String obs = request != null ? blankToNull(request.getObservaciones()) : null;
        appendObservacion(liq, obs, "Anulación de pago");
        actualizarEstadoSegunItems(liq);

        liquidacionRepository.save(liq);
        return mapLiquidacion(
                liquidacionRepository.findByIdWithItems(liq.getId()).orElse(liq),
                true);
    }

    @Transactional
    public LiquidacionSueldoResponse recalcular(Long liquidacionId) {
        liquidacionRepository.findByIdForUpdate(liquidacionId)
                .orElseThrow(() -> new BadRequestException("Liquidación no encontrada"));
        LiquidacionSueldo liq = liquidacionRepository.findByIdWithItems(liquidacionId)
                .orElseThrow(() -> new BadRequestException("Liquidación no encontrada"));
        if (liq.getEstado() == LiquidacionSueldo.EstadoLiquidacion.CANCELADA) {
            throw new BadRequestException("No se puede recalcular una liquidación cancelada");
        }
        assertAccesoLiquidacion(liq);

        List<LiquidacionSueldoItem> pendientes = liq.getItems().stream()
                .filter(i -> i.getEstado() == LiquidacionSueldoItem.EstadoItem.PENDIENTE)
                .toList();
        if (pendientes.isEmpty()) {
            throw new BadRequestException(
                    "No hay ítems pendientes para recalcular (anulá pagos primero si hace falta)");
        }

        LocalDate desde = liq.getFechaDesde();
        LocalDate hasta = liq.getFechaHasta();
        LocalDateTime desdeDt = desde.atStartOfDay();
        LocalDateTime hastaExclusivo = hasta.plusDays(1).atStartOfDay();
        Long sucursalId = liq.getSucursal() != null ? liq.getSucursal().getId() : null;

        for (LiquidacionSueldoItem item : pendientes) {
            EmpleadoSueldoConfig cfg = configRepository.findByEmpleado_Id(item.getEmpleado().getId())
                    .orElseThrow(() -> new BadRequestException(
                            "Sin configuración de sueldo para "
                                    + nombreEmpleado(item.getEmpleado())));
            BigDecimal sueldoBruto = SueldoCalculoHelper.prorratearSueldoFijo(
                    cfg.getSueldoFijo(),
                    cfg.getPeriodoBase(),
                    desde,
                    hasta,
                    diasLaborablesParaCalculo(cfg));
            int diasDesc = item.getDiasDescontados() != null ? item.getDiasDescontados() : 0;
            BigDecimal sueldoBase = SueldoCalculoHelper.aplicarDiasDescontados(
                    sueldoBruto,
                    cfg.getSueldoFijo(),
                    cfg.getPeriodoBase(),
                    desde,
                    hasta,
                    diasLaborablesParaCalculo(cfg),
                    diasDesc);
            BigDecimal ventas = nz(itemRepository.sumVentasEmpleado(
                    item.getEmpleado().getId(), desdeDt, hastaExclusivo, sucursalId));
            long cant = itemRepository.countVentasEmpleado(
                    item.getEmpleado().getId(), desdeDt, hastaExclusivo, sucursalId);
            long unidades = itemRepository.sumUnidadesEmpleado(
                    item.getEmpleado().getId(), desdeDt, hastaExclusivo, sucursalId);
            BigDecimal pct = liq.getPorcentajeComisionOverride() != null
                    ? liq.getPorcentajeComisionOverride()
                    : nz(cfg.getPorcentajeComision());
            BigDecimal comision = SueldoCalculoHelper.comision(ventas, pct);

            item.setSueldoBase(sueldoBase);
            item.setVentasTotal(ventas);
            item.setCantidadVentas((int) cant);
            item.setCantidadArticulos((int) unidades);
            item.setPorcentajeComision(pct);
            item.setComisionMonto(comision);
            item.setTotal(sueldoBase.add(comision));
        }

        recalcularTotalesCabecera(liq);
        appendObservacion(liq, null, "Recálculo de ítems pendientes");
        if (liq.getEstado() == LiquidacionSueldo.EstadoLiquidacion.BORRADOR) {
            liq.setEstado(LiquidacionSueldo.EstadoLiquidacion.CALCULADA);
        }

        liquidacionRepository.save(liq);
        return mapLiquidacion(
                liquidacionRepository.findByIdWithItems(liq.getId()).orElse(liq),
                true);
    }

    @Transactional
    public LiquidacionSueldoResponse cancelar(Long id) {
        LiquidacionSueldo liq = liquidacionRepository.findByIdWithItems(id)
                .orElseThrow(() -> new BadRequestException("Liquidación no encontrada"));
        assertAccesoLiquidacion(liq);
        boolean hayPagados = liq.getItems().stream()
                .anyMatch(i -> i.getEstado() == LiquidacionSueldoItem.EstadoItem.PAGADO);
        if (hayPagados) {
            throw new BadRequestException("No se puede cancelar: ya hay ítems pagados (anulá los pagos primero)");
        }
        liq.setEstado(LiquidacionSueldo.EstadoLiquidacion.CANCELADA);
        return mapLiquidacion(liquidacionRepository.save(liq), true);
    }

    @Transactional
    public LiquidacionSueldoResponse actualizarDiasDescontados(
            Long liquidacionId,
            Long itemId,
            LiquidacionSueldoItemDiasDescontadosRequest request
    ) {
        if (request == null || request.getDiasDescontados() == null) {
            throw new BadRequestException("Indicá diasDescontados");
        }
        int dias = request.getDiasDescontados();
        if (dias < 0) {
            throw new BadRequestException("diasDescontados no puede ser negativo");
        }
        if (dias > 366) {
            throw new BadRequestException("diasDescontados demasiado alto");
        }

        LiquidacionSueldo liq = liquidacionRepository.findByIdWithItems(liquidacionId)
                .orElseThrow(() -> new BadRequestException("Liquidación no encontrada"));
        assertAccesoLiquidacion(liq);
        if (liq.getEstado() == LiquidacionSueldo.EstadoLiquidacion.CANCELADA) {
            throw new BadRequestException("La liquidación está cancelada");
        }

        LiquidacionSueldoItem item = liq.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Ítem no encontrado en la liquidación"));
        if (item.getEstado() == LiquidacionSueldoItem.EstadoItem.PAGADO) {
            throw new BadRequestException("No se puede modificar un ítem ya pagado (anulá el pago primero)");
        }

        EmpleadoSueldoConfig cfg = configRepository.findByEmpleado_Id(item.getEmpleado().getId())
                .orElseThrow(() -> new BadRequestException(
                        "Sin configuración de sueldo para " + nombreEmpleado(item.getEmpleado())));

        int maxDias = diasBaseParaDescuento(cfg, liq.getFechaDesde(), liq.getFechaHasta());
        if (dias > maxDias) {
            throw new BadRequestException(
                    "No podés descontar más de " + maxDias + " día(s) en este período");
        }

        item.setDiasDescontados(dias);
        BigDecimal sueldoBase = calcularSueldoBase(
                cfg, liq.getFechaDesde(), liq.getFechaHasta(), true, dias);
        item.setSueldoBase(sueldoBase);
        item.setTotal(sueldoBase.add(nz(item.getComisionMonto())));
        recalcularTotalesCabecera(liq);
        appendObservacion(
                liq,
                nombreEmpleado(item.getEmpleado()) + " → " + dias + " día(s)",
                "Días descontados");
        liquidacionRepository.save(liq);
        return mapLiquidacion(
                liquidacionRepository.findByIdWithItems(liq.getId()).orElse(liq),
                true);
    }

    private void armarItems(LiquidacionSueldo liq, List<EmpleadoSueldoConfig> configs, boolean prorratear) {
        LocalDate desde = liq.getFechaDesde();
        LocalDate hasta = liq.getFechaHasta();
        LocalDateTime desdeDt = desde.atStartOfDay();
        LocalDateTime hastaExclusivo = hasta.plusDays(1).atStartOfDay();
        Long sucursalId = liq.getSucursal() != null ? liq.getSucursal().getId() : null;

        BigDecimal totalSueldos = BigDecimal.ZERO;
        BigDecimal totalComisiones = BigDecimal.ZERO;
        BigDecimal totalGeneral = BigDecimal.ZERO;

        for (EmpleadoSueldoConfig cfg : configs) {
            BigDecimal sueldoBase = calcularSueldoBase(cfg, desde, hasta, prorratear, 0);
            BigDecimal ventas = nz(itemRepository.sumVentasEmpleado(
                    cfg.getEmpleado().getId(), desdeDt, hastaExclusivo, sucursalId));
            long cant = itemRepository.countVentasEmpleado(
                    cfg.getEmpleado().getId(), desdeDt, hastaExclusivo, sucursalId);
            long unidades = itemRepository.sumUnidadesEmpleado(
                    cfg.getEmpleado().getId(), desdeDt, hastaExclusivo, sucursalId);
            BigDecimal pct = liq.getPorcentajeComisionOverride() != null
                    ? liq.getPorcentajeComisionOverride()
                    : nz(cfg.getPorcentajeComision());
            BigDecimal comision = SueldoCalculoHelper.comision(ventas, pct);
            BigDecimal total = sueldoBase.add(comision);

            LiquidacionSueldoItem item = new LiquidacionSueldoItem();
            item.setLiquidacion(liq);
            item.setEmpleado(cfg.getEmpleado());
            item.setSueldoBase(sueldoBase);
            item.setDiasDescontados(0);
            item.setVentasTotal(ventas);
            item.setCantidadVentas((int) cant);
            item.setCantidadArticulos((int) unidades);
            item.setPorcentajeComision(pct);
            item.setComisionMonto(comision);
            item.setTotal(total);
            item.setEstado(LiquidacionSueldoItem.EstadoItem.PENDIENTE);
            liq.getItems().add(item);

            totalSueldos = totalSueldos.add(sueldoBase);
            totalComisiones = totalComisiones.add(comision);
            totalGeneral = totalGeneral.add(total);
        }

        liq.setTotalSueldos(totalSueldos);
        liq.setTotalComisiones(totalComisiones);
        liq.setTotalGeneral(totalGeneral);
    }

    private void recalcularTotalesCabecera(LiquidacionSueldo liq) {
        BigDecimal totalSueldos = BigDecimal.ZERO;
        BigDecimal totalComisiones = BigDecimal.ZERO;
        BigDecimal totalGeneral = BigDecimal.ZERO;
        for (LiquidacionSueldoItem item : liq.getItems()) {
            totalSueldos = totalSueldos.add(nz(item.getSueldoBase()));
            totalComisiones = totalComisiones.add(nz(item.getComisionMonto()));
            totalGeneral = totalGeneral.add(nz(item.getTotal()));
        }
        liq.setTotalSueldos(totalSueldos);
        liq.setTotalComisiones(totalComisiones);
        liq.setTotalGeneral(totalGeneral);
    }

    private void actualizarEstadoSegunItems(LiquidacionSueldo liq) {
        boolean todosPagados = liq.getItems().stream()
                .allMatch(i -> i.getEstado() == LiquidacionSueldoItem.EstadoItem.PAGADO);
        boolean algunoPagado = liq.getItems().stream()
                .anyMatch(i -> i.getEstado() == LiquidacionSueldoItem.EstadoItem.PAGADO);
        if (todosPagados && !liq.getItems().isEmpty()) {
            liq.setEstado(LiquidacionSueldo.EstadoLiquidacion.PAGADA);
        } else if (algunoPagado) {
            liq.setEstado(LiquidacionSueldo.EstadoLiquidacion.PAGADA_PARCIAL);
        } else {
            liq.setEstado(LiquidacionSueldo.EstadoLiquidacion.CALCULADA);
        }
    }

    private void appendObservacion(LiquidacionSueldo liq, String detalle, String evento) {
        String stamp = LocalDateTime.now().toString();
        String linea = "[" + stamp + "] " + evento
                + (detalle != null ? ": " + detalle : "")
                + " (" + usuarioActualNombre() + ")";
        if (liq.getObservaciones() == null || liq.getObservaciones().isBlank()) {
            liq.setObservaciones(linea);
        } else {
            liq.setObservaciones(liq.getObservaciones().trim() + "\n" + linea);
        }
    }

    private void assertAccesoLiquidacion(LiquidacionSueldo liq) {
        if (liq.getSucursal() != null) {
            sucursalScopeService.assertCanAccess(liq.getSucursal().getId());
            return;
        }
        if (!sucursalScopeService.hasGlobalAccess()) {
            throw new ForbiddenException("No tiene acceso a liquidaciones sin sucursal");
        }
    }

    private List<EmpleadoSueldoConfig> seleccionarConfigs(List<Long> empleadoIds) {
        List<EmpleadoSueldoConfig> activas = configRepository.findActivas();
        if (empleadoIds == null || empleadoIds.isEmpty()) {
            return activas;
        }
        Set<Long> ids = new HashSet<>(empleadoIds);
        List<EmpleadoSueldoConfig> seleccionadas = activas.stream()
                .filter(c -> ids.contains(c.getEmpleado().getId()))
                .toList();
        Set<Long> encontrados = seleccionadas.stream()
                .map(c -> c.getEmpleado().getId())
                .collect(Collectors.toSet());
        List<Long> faltantes = ids.stream().filter(id -> !encontrados.contains(id)).sorted().toList();
        if (!faltantes.isEmpty()) {
            throw new BadRequestException(
                    "Empleados sin configuración de sueldo activa o inexistentes: " + faltantes);
        }
        return seleccionadas;
    }

    private LocalDate[] resolverRango(PeriodoSueldo periodo, LocalDate desdeReq, LocalDate hastaReq) {
        LocalDate hoy = LocalDate.now();
        return switch (periodo) {
            case DIA -> {
                LocalDate d = desdeReq != null ? desdeReq : hoy;
                yield new LocalDate[]{d, d};
            }
            case SEMANA -> {
                LocalDate d = desdeReq != null ? desdeReq : hoy.minusDays(hoy.getDayOfWeek().getValue() - 1L);
                LocalDate h = hastaReq != null ? hastaReq : d.plusDays(6);
                yield new LocalDate[]{d, h};
            }
            case QUINCENA -> {
                if (desdeReq != null && hastaReq != null) {
                    yield new LocalDate[]{desdeReq, hastaReq};
                }
                LocalDate d = desdeReq != null ? desdeReq : (hoy.getDayOfMonth() <= 15
                        ? hoy.withDayOfMonth(1)
                        : hoy.withDayOfMonth(16));
                LocalDate h = hastaReq != null ? hastaReq : (d.getDayOfMonth() <= 15
                        ? d.withDayOfMonth(15)
                        : d.withDayOfMonth(d.lengthOfMonth()));
                yield new LocalDate[]{d, h};
            }
            case MES -> {
                LocalDate d = desdeReq != null ? desdeReq : hoy.withDayOfMonth(1);
                LocalDate h = hastaReq != null ? hastaReq : d.withDayOfMonth(d.lengthOfMonth());
                yield new LocalDate[]{d, h};
            }
            case PERSONALIZADO -> {
                if (desdeReq == null || hastaReq == null) {
                    throw new BadRequestException("Para período PERSONALIZADO indique fechaDesde y fechaHasta");
                }
                yield new LocalDate[]{desdeReq, hastaReq};
            }
        };
    }

    private List<Integer> diasLaborablesParaCalculo(EmpleadoSueldoConfig cfg) {
        List<Integer> dias = DiasLaborablesCodec.parse(cfg.getDiasLaborables());
        if (cfg.getPeriodoBase() == PeriodoSueldo.DIA && dias.isEmpty()) {
            return DiasLaborablesCodec.defaultLunesAViernes();
        }
        return dias;
    }

    private BigDecimal calcularSueldoBase(
            EmpleadoSueldoConfig cfg,
            LocalDate desde,
            LocalDate hasta,
            boolean prorratear,
            int diasDescontados
    ) {
        BigDecimal bruto = prorratear
                ? SueldoCalculoHelper.prorratearSueldoFijo(
                cfg.getSueldoFijo(),
                cfg.getPeriodoBase(),
                desde,
                hasta,
                diasLaborablesParaCalculo(cfg))
                : nz(cfg.getSueldoFijo());
        if (!prorratear) {
            // Sin prorrateo: cada día descontado resta un día de sueldo diario
            // (o proporción calendario si no es base DIA).
            return SueldoCalculoHelper.aplicarDiasDescontados(
                    bruto,
                    cfg.getSueldoFijo(),
                    cfg.getPeriodoBase() == PeriodoSueldo.DIA ? PeriodoSueldo.DIA : PeriodoSueldo.MES,
                    desde,
                    hasta,
                    diasLaborablesParaCalculo(cfg),
                    diasDescontados);
        }
        return SueldoCalculoHelper.aplicarDiasDescontados(
                bruto,
                cfg.getSueldoFijo(),
                cfg.getPeriodoBase(),
                desde,
                hasta,
                diasLaborablesParaCalculo(cfg),
                diasDescontados);
    }

    private int diasBaseParaDescuento(EmpleadoSueldoConfig cfg, LocalDate desde, LocalDate hasta) {
        if (cfg.getPeriodoBase() == PeriodoSueldo.DIA) {
            return (int) SueldoCalculoHelper.contarDiasLaborables(
                    desde,
                    hasta,
                    SueldoCalculoHelper.resolverDiasLaborables(diasLaborablesParaCalculo(cfg)));
        }
        return (int) ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    private void validarPorcentajeComision(BigDecimal pct) {
        if (pct.compareTo(BigDecimal.ZERO) < 0 || pct.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("El porcentaje de comisión debe estar entre 0 y 100");
        }
    }

    private EmpleadoSueldoConfigResponse mapConfig(EmpleadoSueldoConfig c) {
        Empleado e = c.getEmpleado();
        String roles = "";
        if (e.getUsuario() != null) {
            roles = roleRepository.findAllByUsuariosHasRoles_Usuario_Id(e.getUsuario().getId())
                    .stream().map(Role::getNombre).collect(Collectors.joining(", "));
        }
        return EmpleadoSueldoConfigResponse.builder()
                .id(c.getId())
                .empleadoId(e.getId())
                .empleadoNombre(nombreEmpleado(e))
                .roles(roles)
                .sueldoFijo(c.getSueldoFijo())
                .periodoBase(c.getPeriodoBase())
                .porcentajeComision(c.getPorcentajeComision())
                .diasLaborables(DiasLaborablesCodec.parse(c.getDiasLaborables()))
                .activo(c.getActivo())
                .observaciones(c.getObservaciones())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private LiquidacionSueldoResponse mapLiquidacion(LiquidacionSueldo l, boolean conItems) {
        List<LiquidacionSueldoResponse.Item> items = List.of();
        if (conItems && l.getItems() != null) {
            items = l.getItems().stream().map(i -> mapItem(i, l)).toList();
        }
        return LiquidacionSueldoResponse.builder()
                .id(l.getId())
                .numero(l.getNumero())
                .sucursalId(l.getSucursal() != null ? l.getSucursal().getId() : null)
                .sucursalNombre(l.getSucursal() != null ? l.getSucursal().getNombre() : null)
                .periodoTipo(l.getPeriodoTipo())
                .fechaDesde(l.getFechaDesde())
                .fechaHasta(l.getFechaHasta())
                .porcentajeComisionOverride(l.getPorcentajeComisionOverride())
                .estado(l.getEstado())
                .totalSueldos(l.getTotalSueldos())
                .totalComisiones(l.getTotalComisiones())
                .totalGeneral(l.getTotalGeneral())
                .responsable(l.getResponsable())
                .observaciones(l.getObservaciones())
                .createdAt(l.getCreatedAt())
                .items(items)
                .build();
    }

    private LiquidacionSueldoResponse.Item mapItem(LiquidacionSueldoItem i, LiquidacionSueldo l) {
        Integer diasLab = null;
        var cfgOpt = configRepository.findByEmpleado_Id(i.getEmpleado().getId());
        if (cfgOpt.isPresent()) {
            diasLab = diasBaseParaDescuento(cfgOpt.get(), l.getFechaDesde(), l.getFechaHasta());
        }
        return LiquidacionSueldoResponse.Item.builder()
                .id(i.getId())
                .empleadoId(i.getEmpleado().getId())
                .empleadoNombre(nombreEmpleado(i.getEmpleado()))
                .sueldoBase(i.getSueldoBase())
                .diasDescontados(i.getDiasDescontados() != null ? i.getDiasDescontados() : 0)
                .diasLaborablesPeriodo(diasLab)
                .ventasTotal(i.getVentasTotal())
                .cantidadVentas(i.getCantidadVentas())
                .cantidadArticulos(i.getCantidadArticulos())
                .porcentajeComision(i.getPorcentajeComision())
                .comisionMonto(i.getComisionMonto())
                .total(i.getTotal())
                .estado(i.getEstado())
                .cuentaPagoId(i.getCuentaPago() != null ? i.getCuentaPago().getId() : null)
                .cuentaPagoNombre(i.getCuentaPago() != null ? i.getCuentaPago().getNombre() : null)
                .movimientoId(i.getMovimiento() != null ? i.getMovimiento().getId() : null)
                .fechaPago(i.getFechaPago())
                .build();
    }

    private String nombreEmpleado(Empleado e) {
        String n = ((e.getNombre() != null ? e.getNombre() : "") + " "
                + (e.getApellido() != null ? e.getApellido() : "")).trim();
        return n.isEmpty() ? ("Empleado #" + e.getId()) : n;
    }

    private String usuarioActualNombre() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails details) {
            return details.getUsername();
        }
        return auth != null ? auth.getName() : "sistema";
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
