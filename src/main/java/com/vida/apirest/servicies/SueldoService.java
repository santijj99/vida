package com.vida.apirest.servicies;

import com.vida.apirest.dto.sueldo.EmpleadoSueldoConfigRequest;
import com.vida.apirest.dto.sueldo.EmpleadoSueldoConfigResponse;
import com.vida.apirest.dto.sueldo.LiquidacionSueldoCreateRequest;
import com.vida.apirest.dto.sueldo.LiquidacionSueldoPagoRequest;
import com.vida.apirest.dto.sueldo.LiquidacionSueldoResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            throw new RuntimeException("empleadoId es obligatorio");
        }
        Empleado empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        EmpleadoSueldoConfig config = configRepository.findByEmpleado_Id(empleado.getId())
                .orElseGet(EmpleadoSueldoConfig::new);
        config.setEmpleado(empleado);
        config.setSueldoFijo(nz(request.getSueldoFijo()));
        config.setPeriodoBase(request.getPeriodoBase() != null ? request.getPeriodoBase() : PeriodoSueldo.MES);
        config.setPorcentajeComision(nz(request.getPorcentajeComision()));
        config.setActivo(request.getActivo() == null || request.getActivo());
        config.setObservaciones(blankToNull(request.getObservaciones()));
        return mapConfig(configRepository.save(config));
    }

    @Transactional(readOnly = true)
    public List<LiquidacionSueldoResponse> listarLiquidaciones(Long sucursalId) {
        Long filtro = sucursalId != null ? sucursalScopeService.enforceFilter(sucursalId) : null;
        if (filtro != null) {
            sucursalScopeService.assertCanAccess(filtro);
        }
        // Incluye items para poder mostrar avance de pagos (día/semana/etc.) en el FRONT.
        return liquidacionRepository.listar(filtro).stream()
                .map(l -> mapLiquidacion(l, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public LiquidacionSueldoResponse obtenerLiquidacion(Long id) {
        LiquidacionSueldo liq = liquidacionRepository.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Liquidación no encontrada"));
        if (liq.getSucursal() != null) {
            sucursalScopeService.assertCanAccess(liq.getSucursal().getId());
        }
        return mapLiquidacion(liq, true);
    }

    @Transactional
    public LiquidacionSueldoResponse crearLiquidacion(LiquidacionSueldoCreateRequest request) {
        PeriodoSueldo periodo = request.getPeriodoTipo() != null ? request.getPeriodoTipo() : PeriodoSueldo.MES;
        LocalDate[] rango = resolverRango(periodo, request.getFechaDesde(), request.getFechaHasta());
        LocalDate desde = rango[0];
        LocalDate hasta = rango[1];
        if (hasta.isBefore(desde)) {
            throw new RuntimeException("fechaHasta no puede ser anterior a fechaDesde");
        }

        Sucursal sucursal = null;
        if (request.getSucursalId() != null) {
            sucursalScopeService.assertCanUse(request.getSucursalId());
            sucursal = sucursalRepository.findById(request.getSucursalId())
                    .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));
        }

        List<EmpleadoSueldoConfig> configs = seleccionarConfigs(request.getEmpleadoIds());
        if (configs.isEmpty()) {
            throw new RuntimeException("No hay empleados con configuración de sueldo activa");
        }

        boolean permitirSolapamiento = Boolean.TRUE.equals(request.getPermitirSolapamiento());
        if (!permitirSolapamiento) {
            Long sucursalIdCheck = sucursal != null ? sucursal.getId() : null;
            long solapes = liquidacionRepository.countSolapadas(
                    desde, hasta, sucursalIdCheck, LiquidacionSueldo.EstadoLiquidacion.CANCELADA);
            if (solapes > 0) {
                throw new RuntimeException(
                        "Ya existe una liquidación activa que se solapa con "
                                + desde + " → " + hasta
                                + ". Cancelala, elegí otro rango o marcá \"permitir solapamiento\" "
                                + "si estás seguro de no pagar dos veces las mismas ventas.");
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

        LocalDateTime desdeDt = desde.atStartOfDay();
        LocalDateTime hastaExclusivo = hasta.plusDays(1).atStartOfDay();
        Long sucursalId = sucursal != null ? sucursal.getId() : null;

        BigDecimal totalSueldos = BigDecimal.ZERO;
        BigDecimal totalComisiones = BigDecimal.ZERO;
        BigDecimal totalGeneral = BigDecimal.ZERO;

        boolean prorratear = request.getProrratearSueldo() == null || request.getProrratearSueldo();
        for (EmpleadoSueldoConfig cfg : configs) {
            BigDecimal sueldoBase = prorratear
                    ? calcularSueldoProrrateado(cfg, desde, hasta)
                    : nz(cfg.getSueldoFijo());
            BigDecimal ventas = nz(itemRepository.sumVentasEmpleado(
                    cfg.getEmpleado().getId(), desdeDt, hastaExclusivo, sucursalId));
            long cant = itemRepository.countVentasEmpleado(
                    cfg.getEmpleado().getId(), desdeDt, hastaExclusivo, sucursalId);
            long unidades = itemRepository.sumUnidadesEmpleado(
                    cfg.getEmpleado().getId(), desdeDt, hastaExclusivo, sucursalId);
            BigDecimal pct = request.getPorcentajeComisionOverride() != null
                    ? request.getPorcentajeComisionOverride()
                    : nz(cfg.getPorcentajeComision());
            BigDecimal comision = ventas.multiply(pct)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal total = sueldoBase.add(comision);

            LiquidacionSueldoItem item = new LiquidacionSueldoItem();
            item.setLiquidacion(liq);
            item.setEmpleado(cfg.getEmpleado());
            item.setSueldoBase(sueldoBase);
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

        LiquidacionSueldo saved = liquidacionRepository.save(liq);
        return mapLiquidacion(
                liquidacionRepository.findByIdWithItems(saved.getId()).orElse(saved),
                true);
    }

    @Transactional
    public LiquidacionSueldoResponse pagar(Long liquidacionId, LiquidacionSueldoPagoRequest request) {
        if (request.getCuentaId() == null) {
            throw new RuntimeException("cuentaId es obligatorio para pagar");
        }
        LiquidacionSueldo liq = liquidacionRepository.findByIdWithItems(liquidacionId)
                .orElseThrow(() -> new RuntimeException("Liquidación no encontrada"));
        if (liq.getEstado() == LiquidacionSueldo.EstadoLiquidacion.CANCELADA) {
            throw new RuntimeException("La liquidación está cancelada");
        }
        if (liq.getEstado() == LiquidacionSueldo.EstadoLiquidacion.PAGADA) {
            throw new RuntimeException("La liquidación ya está pagada");
        }
        if (liq.getSucursal() != null) {
            sucursalScopeService.assertCanAccess(liq.getSucursal().getId());
        }

        CuentaFinanciera cuenta = cuentaRepository.findById(request.getCuentaId())
                .orElseThrow(() -> new RuntimeException("Cuenta financiera no encontrada"));
        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            throw new RuntimeException("La cuenta financiera no está activa");
        }
        if (liq.getSucursal() != null
                && !cuenta.getSucursal().getId().equals(liq.getSucursal().getId())) {
            throw new RuntimeException("La cuenta debe pertenecer a la misma sucursal de la liquidación");
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
                throw new RuntimeException("Uno o más ítems no pertenecen a esta liquidación");
            }
        }
        if (aPagar.isEmpty()) {
            throw new RuntimeException("No hay ítems pendientes para pagar");
        }

        LocalDateTime ahora = LocalDateTime.now();
        for (LiquidacionSueldoItem item : aPagar) {
            if (item.getEstado() == LiquidacionSueldoItem.EstadoItem.PAGADO) {
                continue;
            }
            if (item.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                cajaMovimientoService.registrarEgreso(
                        cuenta,
                        item.getTotal(),
                        "Sueldo/comisión " + nombreEmpleado(item.getEmpleado()) + " (" + liq.getNumero() + ")",
                        liq.getNumero());
            }
            item.setEstado(LiquidacionSueldoItem.EstadoItem.PAGADO);
            item.setFechaPago(ahora);
            item.setCuentaPago(cuenta);
        }

        boolean todosPagados = liq.getItems().stream()
                .allMatch(i -> i.getEstado() == LiquidacionSueldoItem.EstadoItem.PAGADO);
        boolean algunoPagado = liq.getItems().stream()
                .anyMatch(i -> i.getEstado() == LiquidacionSueldoItem.EstadoItem.PAGADO);
        if (todosPagados) {
            liq.setEstado(LiquidacionSueldo.EstadoLiquidacion.PAGADA);
        } else if (algunoPagado) {
            liq.setEstado(LiquidacionSueldo.EstadoLiquidacion.PAGADA_PARCIAL);
        }

        liquidacionRepository.save(liq);
        return mapLiquidacion(
                liquidacionRepository.findByIdWithItems(liq.getId()).orElse(liq),
                true);
    }

    @Transactional
    public LiquidacionSueldoResponse cancelar(Long id) {
        LiquidacionSueldo liq = liquidacionRepository.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Liquidación no encontrada"));
        boolean hayPagados = liq.getItems().stream()
                .anyMatch(i -> i.getEstado() == LiquidacionSueldoItem.EstadoItem.PAGADO);
        if (hayPagados) {
            throw new RuntimeException("No se puede cancelar: ya hay ítems pagados");
        }
        liq.setEstado(LiquidacionSueldo.EstadoLiquidacion.CANCELADA);
        return mapLiquidacion(liquidacionRepository.save(liq), true);
    }

    private List<EmpleadoSueldoConfig> seleccionarConfigs(List<Long> empleadoIds) {
        List<EmpleadoSueldoConfig> activas = configRepository.findActivas();
        if (empleadoIds == null || empleadoIds.isEmpty()) {
            return activas;
        }
        Set<Long> ids = new HashSet<>(empleadoIds);
        return activas.stream().filter(c -> ids.contains(c.getEmpleado().getId())).toList();
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
                    throw new RuntimeException("Para período PERSONALIZADO indique fechaDesde y fechaHasta");
                }
                yield new LocalDate[]{desdeReq, hastaReq};
            }
        };
    }

    /**
     * Prorratea el sueldo fijo del empleado al rango liquidado.
     * Ej: sueldo mensual $300.000 liquidando 10 días → 300000 * 10 / díasDelMes.
     */
    private BigDecimal calcularSueldoProrrateado(EmpleadoSueldoConfig cfg, LocalDate desde, LocalDate hasta) {
        BigDecimal fijo = nz(cfg.getSueldoFijo());
        if (fijo.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        long diasLiquidacion = ChronoUnit.DAYS.between(desde, hasta) + 1;
        if (diasLiquidacion <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal diasBase = switch (cfg.getPeriodoBase()) {
            case DIA -> BigDecimal.ONE;
            case SEMANA -> BigDecimal.valueOf(7);
            case QUINCENA -> BigDecimal.valueOf(15);
            case MES, PERSONALIZADO -> BigDecimal.valueOf(desde.lengthOfMonth());
        };
        return fijo.multiply(BigDecimal.valueOf(diasLiquidacion))
                .divide(diasBase, 2, RoundingMode.HALF_UP);
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
                .activo(c.getActivo())
                .observaciones(c.getObservaciones())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private LiquidacionSueldoResponse mapLiquidacion(LiquidacionSueldo l, boolean conItems) {
        List<LiquidacionSueldoResponse.Item> items = List.of();
        if (conItems && l.getItems() != null) {
            items = l.getItems().stream().map(i -> LiquidacionSueldoResponse.Item.builder()
                    .id(i.getId())
                    .empleadoId(i.getEmpleado().getId())
                    .empleadoNombre(nombreEmpleado(i.getEmpleado()))
                    .sueldoBase(i.getSueldoBase())
                    .ventasTotal(i.getVentasTotal())
                    .cantidadVentas(i.getCantidadVentas())
                    .cantidadArticulos(i.getCantidadArticulos())
                    .porcentajeComision(i.getPorcentajeComision())
                    .comisionMonto(i.getComisionMonto())
                    .total(i.getTotal())
                    .estado(i.getEstado())
                    .cuentaPagoId(i.getCuentaPago() != null ? i.getCuentaPago().getId() : null)
                    .cuentaPagoNombre(i.getCuentaPago() != null ? i.getCuentaPago().getNombre() : null)
                    .fechaPago(i.getFechaPago())
                    .build()).toList();
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
