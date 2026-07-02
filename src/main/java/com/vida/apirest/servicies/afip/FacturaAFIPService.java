package com.vida.apirest.servicies.afip;

import com.vida.apirest.config.AfipProperties;
import com.vida.apirest.dto.afip.EmitirFacturaAFIPRequest;
import com.vida.apirest.dto.afip.FacturaAFIPResponse;
import com.vida.apirest.dto.venta.PagoVentaRequest;
import com.vida.apirest.model.afip.*;
import com.vida.apirest.model.credito.Credito;
import com.vida.apirest.model.credito.Cuota;
import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.model.persona.Direccion;
import com.vida.apirest.model.venta.PagoVenta;
import com.vida.apirest.model.venta.Venta;
import com.vida.apirest.model.venta.VentaDetalle;
import com.vida.apirest.repositories.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacturaAFIPService {

    private static final Logger log = LoggerFactory.getLogger(FacturaAFIPService.class);
    private static final DateTimeFormatter CBTE_FCH = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AfipProperties afipProperties;
    private final AfipContextService afipContextService;
    private final WSFEService wsfeService;
    private final TicketPDFService ticketPDFService;
    private final FacturaAFIPRepository facturaAFIPRepository;
    private final FacturaItemAFIPRepository facturaItemAFIPRepository;
    private final FacturaIvaAFIPRepository facturaIvaAFIPRepository;
    private final ClienteAFIPRepository clienteAFIPRepository;
    private final CAERepository caeRepository;
    private final VentaRepository ventaRepository;
    private final CreditoRepository creditoRepository;

    @Transactional
    public FacturaAFIP emitirFactura(Long ventaId) throws Exception {
        return emitirFactura(ventaId, null);
    }

    @Transactional
    public FacturaAFIP emitirFactura(Long ventaId, EmitirFacturaAFIPRequest request) throws Exception {
        if (!afipProperties.isEnabled()) {
            throw new IllegalStateException("El módulo AFIP está deshabilitado (afip.enabled=false)");
        }

        Venta venta = ventaRepository.findByIdWithDetalles(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + ventaId));

        AfipContext context = afipContextService.resolveForVenta(venta);
        return afipContextService.callWithContext(context, () -> emitirFacturaConContexto(venta, ventaId, request, context));
    }

    private FacturaAFIP emitirFacturaConContexto(
            Venta venta,
            Long ventaId,
            EmitirFacturaAFIPRequest request,
            AfipContext context
    ) throws Exception {

        facturaAFIPRepository.findByVenta_Id(ventaId).ifPresent(existing -> {
            throw new RuntimeException("La venta ya tiene una factura AFIP asociada");
        });

        Integer cbteTipoAUsar = request != null && request.getCbteTipo() != null
                ? request.getCbteTipo()
                : context.cbteTipoDefault();

        ClienteAFIP clienteAFIP = obtenerOCrearClienteAFIP(venta.getCliente(), request, cbteTipoAUsar);
        Long ultimoCbteNro = obtenerUltimoComprobanteAutorizado(context, cbteTipoAUsar);
        Long nuevoCbteNro = ultimoCbteNro + 1;

        BigDecimal montoAFacturar = request != null ? request.getMontoAFacturar() : null;
        FacturaAFIP facturaAFIP = crearFacturaAFIP(venta, clienteAFIP, nuevoCbteNro, cbteTipoAUsar, montoAFacturar, context);

        WSFEService.FECAERequest fecaeRequest = prepararFECAERequest(facturaAFIP);
        WSFEService.FECAEResponse fecaeResponse = wsfeService.solicitarCAE(fecaeRequest);

        facturaAFIP.setCae(fecaeResponse.getCae());
        facturaAFIP.setCaeFchVto(fecaeResponse.getCaeFchVto());
        facturaAFIP.setResultado(fecaeResponse.getResultado());
        facturaAFIP.setMotivos(fecaeResponse.getMotivos());
        facturaAFIP.setObservaciones(fecaeResponse.getObservaciones());

        if ("A".equals(fecaeResponse.getResultado())) {
            actualizarUltimoComprobante(context, nuevoCbteNro, cbteTipoAUsar);
        }

        FacturaAFIP facturaGuardada = facturaAFIPRepository.save(facturaAFIP);

        if ("A".equals(fecaeResponse.getResultado())) {
            try {
                Credito credito = null;
                List<Cuota> cuotas = List.of();
                List<Credito> creditos = creditoRepository.findByVentaIdWithCuotas(ventaId);
                if (!creditos.isEmpty()) {
                    credito = creditos.get(0);
                    cuotas = TicketPDFService.ordenarCuotas(credito.getCuotas());
                }
                byte[] pdf = ticketPDFService.generarTicketPDFBytes(facturaGuardada, credito, cuotas);
                log.info("Ticket PDF generado para factura AFIP {} ({} bytes)", facturaGuardada.getIdFacturaAFIP(), pdf.length);
            } catch (Exception e) {
                log.warn("No se pudo generar el PDF del ticket: {}", e.getMessage());
            }
        }

        log.info("Factura AFIP {} - resultado: {}, CAE: {}", facturaGuardada.getIdFacturaAFIP(),
                fecaeResponse.getResultado(), fecaeResponse.getCae());

        return facturaGuardada;
    }

    @Transactional(readOnly = true)
    public List<FacturaAFIPResponse> listarTodas() {
        return facturaAFIPRepository.findAllByOrderByFechaEmisionDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FacturaAFIPResponse obtenerDetalle(Long id) {
        FacturaAFIP factura = cargarFacturaCompleta(id);
        if (factura == null) {
            throw new RuntimeException("Factura AFIP no encontrada con ID: " + id);
        }
        return toResponse(factura);
    }

    @Transactional(readOnly = true)
    public FacturaAFIPResponse obtenerPorVenta(Long ventaId) {
        FacturaAFIP factura = facturaAFIPRepository.findByVenta_Id(ventaId)
                .map(f -> cargarFacturaCompleta(f.getIdFacturaAFIP()))
                .orElse(null);
        if (factura == null) {
            throw new RuntimeException("No hay factura AFIP para la venta ID: " + ventaId);
        }
        return toResponse(factura);
    }

    @Transactional(readOnly = true)
    public byte[] generarTicketPdf(Long id) throws Exception {
        FacturaAFIP factura = cargarFacturaCompleta(id);
        if (factura == null) {
            throw new RuntimeException("Factura AFIP no encontrada con ID: " + id);
        }
        if (factura.getCae() == null || factura.getCae().isBlank()) {
            throw new RuntimeException("La factura no tiene CAE asignado");
        }
        Venta venta = ventaRepository.findByIdWithDetalles(factura.getVenta().getId())
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        AfipContext context = afipContextService.resolveForVenta(venta);
        Credito credito = null;
        List<Cuota> cuotas = List.of();
        List<Credito> creditos = creditoRepository.findByVentaIdWithCuotas(venta.getId());
        if (!creditos.isEmpty()) {
            credito = creditos.get(0);
            cuotas = TicketPDFService.ordenarCuotas(credito.getCuotas());
        }
        final Credito creditoFinal = credito;
        final List<Cuota> cuotasFinal = cuotas;
        return afipContextService.callWithContext(context,
                () -> ticketPDFService.generarTicketPDFBytes(factura, creditoFinal, cuotasFinal));
    }

    public boolean requiereFacturacionAutomatica(String metodoPago) {
        if (metodoPago == null) {
            return false;
        }
        String normalizado = metodoPago.trim().toUpperCase();
        return normalizado.equals("QR")
                || normalizado.equals("TARJETA DE CREDITO")
                || normalizado.equals("TARJETA DE DEBITO")
                || normalizado.equals("TARJETA CREDITO")
                || normalizado.equals("TARJETA DEBITO");
    }

    @Transactional
    public FacturaAFIP intentarFacturarVenta(Long ventaId) {
        return intentarFacturarVenta(ventaId, null);
    }

    @Transactional
    public FacturaAFIP intentarFacturarVenta(Long ventaId, EmitirFacturaAFIPRequest configOpcional) {
        return intentarFacturarVenta(ventaId, configOpcional, null);
    }

    @Transactional
    public FacturaAFIP intentarFacturarVenta(
            Long ventaId,
            EmitirFacturaAFIPRequest configOpcional,
            List<PagoVentaRequest> pagosRequest
    ) {
        if (!afipProperties.isEnabled() || !afipProperties.isAutoFacturarEnVenta()) {
            return null;
        }

        Venta venta = ventaRepository.findByIdWithDetalles(ventaId).orElse(null);
        if (venta == null) {
            log.warn("Facturación ARCA omitida para venta {}: venta no encontrada", ventaId);
            return null;
        }

        BigDecimal montoArca = resolverMontoAFacturar(venta, configOpcional, pagosRequest);
        if (montoArca.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Venta {} sin pagos ARCA (crédito/débito/QR)", ventaId);
            return null;
        }

        try {
            EmitirFacturaAFIPRequest request = configOpcional != null
                    ? configOpcional
                    : new EmitirFacturaAFIPRequest();
            request.setMontoAFacturar(montoArca);
            log.info("Facturación ARCA venta {}: monto a facturar {} (total venta {})",
                    ventaId, montoArca, venta.getTotal());
            AfipContext context = afipContextService.resolveForVenta(venta);
            return afipContextService.callWithContext(context, () -> emitirFacturaConContexto(venta, ventaId, request, context));
        } catch (Exception e) {
            log.error("Error al facturar automáticamente venta {}: {}", ventaId, e.getMessage());
            return null;
        }
    }

    private BigDecimal resolverMontoAFacturar(
            Venta venta,
            EmitirFacturaAFIPRequest config,
            List<PagoVentaRequest> pagosRequest
    ) {
        BigDecimal desdeEntidad = sumarMontoPagosArcaEntidad(venta.getPagos());
        BigDecimal desdeRequest = sumarMontoPagosArcaRequest(pagosRequest);
        BigDecimal montoCalculado = desdeEntidad.compareTo(BigDecimal.ZERO) > 0
                ? desdeEntidad
                : desdeRequest;

        if (config != null
                && config.getMontoAFacturar() != null
                && config.getMontoAFacturar().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tope = venta.getTotal() != null ? venta.getTotal() : montoCalculado;
            if (tope.compareTo(BigDecimal.ZERO) <= 0) {
                tope = config.getMontoAFacturar();
            }
            return config.getMontoAFacturar().min(tope);
        }
        return montoCalculado;
    }

    private BigDecimal sumarMontoPagosArcaEntidad(Collection<PagoVenta> pagos) {
        if (pagos == null || pagos.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return pagos.stream()
                .filter(p -> p.getMonto() != null && requiereFacturacionAutomatica(p.getMetodoPago()))
                .map(PagoVenta::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumarMontoPagosArcaRequest(List<PagoVentaRequest> pagos) {
        if (pagos == null || pagos.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return pagos.stream()
                .filter(p -> p.getMonto() != null
                        && p.getMonto().compareTo(BigDecimal.ZERO) > 0
                        && requiereFacturacionAutomatica(p.getMetodoPago()))
                .map(PagoVentaRequest::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ClienteAFIP obtenerOCrearClienteAFIP(Cliente cliente, EmitirFacturaAFIPRequest request, Integer cbteTipoAUsar) {
        ClienteAFIP clienteAFIP = clienteAFIPRepository.findByCliente_Id(cliente.getId())
                .orElseGet(() -> {
                    ClienteAFIP nuevo = new ClienteAFIP();
                    nuevo.setCliente(cliente);
                    return nuevo;
                });

        if (request != null && request.getDocTipo() != null) {
            Integer docTipo = request.getDocTipo();
            String docNro = request.getDocNro();
            if (request.getCondicionIVAReceptorId() == null) {
                throw new IllegalStateException("CondicionIVAReceptorId es requerido");
            }
            if (cbteTipoAUsar == 6 && docTipo == 99) {
                docNro = "0";
            } else if (docNro == null || docNro.isBlank()) {
                docNro = docTipo == 99 ? "0" : null;
            }
            if (docNro == null) {
                throw new IllegalStateException("DocNro es requerido para DocTipo " + docTipo);
            }
            clienteAFIP.setDocTipo(docTipo);
            clienteAFIP.setDocNro(docNro);
            clienteAFIP.setRazonSocial(request.getRazonSocial() != null ? request.getRazonSocial() : "Consumidor Final");
            clienteAFIP.setCondicionIVAReceptorId(request.getCondicionIVAReceptorId());
            clienteAFIP.setDomicilio(request.getDomicilio());
        } else {
            ajustarClienteAFIPParaComprobante(clienteAFIP, cliente, cbteTipoAUsar);
        }

        return clienteAFIPRepository.save(clienteAFIP);
    }

    private void ajustarClienteAFIPParaComprobante(ClienteAFIP clienteAFIP, Cliente cliente, Integer cbteTipoAUsar) {
        String razonSocial = "";
        if (cliente.getApellido() != null) {
            razonSocial = cliente.getApellido();
        }
        if (cliente.getNombre() != null) {
            if (!razonSocial.isEmpty()) {
                razonSocial += ", ";
            }
            razonSocial += cliente.getNombre();
        }
        if (razonSocial.isEmpty()) {
            razonSocial = "Consumidor Final";
        }
        clienteAFIP.setRazonSocial(razonSocial);
        clienteAFIP.setDomicilio(formatearDireccion(cliente.getDireccion()));

        if (cbteTipoAUsar == 1) {
            clienteAFIP.setDocTipo(80);
            String cuitCliente = obtenerCuitDelCliente(cliente);
            clienteAFIP.setDocNro(cuitCliente != null ? cuitCliente : "20000000000");
            clienteAFIP.setCondicionIVAReceptorId(1);
        } else if (cbteTipoAUsar == 6 || cbteTipoAUsar == 11) {
            if (cliente.getDni() != null && !cliente.getDni().isBlank()) {
                clienteAFIP.setDocTipo(96);
                clienteAFIP.setDocNro(cliente.getDni());
            } else {
                clienteAFIP.setDocTipo(99);
                clienteAFIP.setDocNro("0");
            }
            clienteAFIP.setCondicionIVAReceptorId(5);
        } else {
            if (cliente.getDni() != null && !cliente.getDni().isBlank()) {
                clienteAFIP.setDocTipo(96);
                clienteAFIP.setDocNro(cliente.getDni());
            } else {
                clienteAFIP.setDocTipo(99);
                clienteAFIP.setDocNro("0");
            }
            clienteAFIP.setCondicionIVAReceptorId(5);
        }
    }

    private String formatearDireccion(Direccion direccion) {
        if (direccion == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (direccion.getCalle() != null) {
            sb.append(direccion.getCalle());
        }
        if (direccion.getNumero() != null) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(direccion.getNumero());
        }
        if (direccion.getLocalidad() != null) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(direccion.getLocalidad());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String obtenerCuitDelCliente(Cliente cliente) {
        if (cliente.getDni() != null && cliente.getDni().length() == 11) {
            try {
                Long.parseLong(cliente.getDni());
                return cliente.getDni();
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Long obtenerUltimoComprobanteAutorizado(AfipContext context, Integer cbteTipoAUsar) throws Exception {
        try {
            Long ultimoAFIP = wsfeService.obtenerUltimoComprobanteAutorizado(context.ptoVta(), cbteTipoAUsar);
            actualizarUltimoComprobante(context, ultimoAFIP, cbteTipoAUsar);
            return ultimoAFIP;
        } catch (Exception e) {
            log.warn("No se pudo consultar AFIP, usando numeración local: {}", e.getMessage());
            return caeRepository.findByEmpresaIdAndPtoVtaAndCbteTipo(context.empresaId(), context.ptoVta(), cbteTipoAUsar)
                    .or(() -> caeRepository.findByPtoVtaAndCbteTipo(context.ptoVta(), cbteTipoAUsar))
                    .map(CAE::getUltimoCbteNro)
                    .orElseGet(() -> {
                        CAE cae = new CAE(context.empresaId(), context.ptoVta(), cbteTipoAUsar, 0L);
                        caeRepository.save(cae);
                        return 0L;
                    });
        }
    }

    private void actualizarUltimoComprobante(AfipContext context, Long numero, Integer cbteTipoAUsar) {
        CAE cae = caeRepository.findByEmpresaIdAndPtoVtaAndCbteTipo(context.empresaId(), context.ptoVta(), cbteTipoAUsar)
                .orElseGet(() -> new CAE(context.empresaId(), context.ptoVta(), cbteTipoAUsar, 0L));
        cae.setEmpresaId(context.empresaId());
        cae.setUltimoCbteNro(numero);
        cae.setFechaActualizacion(new Date());
        caeRepository.save(cae);
    }

    private FacturaAFIP crearFacturaAFIP(Venta venta, ClienteAFIP clienteAFIP, Long cbteNro,
                                         Integer cbteTipoAUsar, BigDecimal montoAFacturar, AfipContext context) {
        FacturaAFIP factura = new FacturaAFIP();
        factura.setVenta(venta);
        factura.setClienteAFIP(clienteAFIP);
        factura.setPtoVta(context.ptoVta());
        factura.setCbteTipo(cbteTipoAUsar);
        factura.setCbteNro(cbteNro);

        LocalDateTime fechaVenta = venta.getFechaVenta() != null ? venta.getFechaVenta() : LocalDateTime.now();
        factura.setCbteFch(fechaVenta.format(CBTE_FCH));

        BigDecimal impTotal = montoAFacturar != null && montoAFacturar.compareTo(BigDecimal.ZERO) > 0
                ? montoAFacturar
                : venta.getTotal();
        factura.setImpTotal(impTotal);

        BigDecimal ratioIVA = BigDecimal.ONE;
        if (montoAFacturar != null && montoAFacturar.compareTo(BigDecimal.ZERO) > 0
                && venta.getTotal().compareTo(BigDecimal.ZERO) > 0) {
            ratioIVA = montoAFacturar.divide(venta.getTotal(), 4, RoundingMode.HALF_UP);
        }

        BigDecimal impNeto;
        BigDecimal impIVA;
        if (venta.getImpuesto() != null && venta.getImpuesto().compareTo(BigDecimal.ZERO) > 0) {
            impNeto = venta.getSubtotal().multiply(ratioIVA).setScale(2, RoundingMode.HALF_UP);
            impIVA = venta.getImpuesto().multiply(ratioIVA).setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal divisor = new BigDecimal("1.21");
            impNeto = impTotal.divide(divisor, 2, RoundingMode.HALF_UP);
            impIVA = impTotal.subtract(impNeto);
        }

        factura.setImpNeto(impNeto);
        factura.setImpIVA(impIVA);
        factura.setImpTotConc(BigDecimal.ZERO);
        factura.setImpOpEx(BigDecimal.ZERO);
        factura.setImpTrib(BigDecimal.ZERO);
        factura.setConcepto(1);
        factura.setMonId("PES");
        factura.setMonCotiz(BigDecimal.ONE);
        factura.setFechaEmision(Date.from(fechaVenta.atZone(ZoneId.systemDefault()).toInstant()));

        BigDecimal ratio = ratioIVA;
        List<FacturaItemAFIP> items = new ArrayList<>();
        if (venta.getDetalles() != null) {
            for (VentaDetalle detalle : venta.getDetalles()) {
                FacturaItemAFIP item = new FacturaItemAFIP();
                item.setFacturaAFIP(factura);
                item.setDescripcion(detalle.getArticulo().getModelo() != null
                        ? detalle.getArticulo().getModelo()
                        : "Artículo " + detalle.getArticulo().getId());
                item.setCantidad(new BigDecimal(detalle.getCantidad()));
                item.setPrecioUnitario(detalle.getPrecioUnitario().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
                item.setSubtotal(detalle.getSubtotal().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
                item.setCodigo(detalle.getArticulo().getCodigo());
                items.add(item);
            }
        }
        factura.setItems(items);

        List<FacturaIvaAFIP> ivas = new ArrayList<>();
        if (factura.getImpNeto().compareTo(BigDecimal.ZERO) > 0) {
            FacturaIvaAFIP iva = new FacturaIvaAFIP();
            iva.setFacturaAFIP(factura);
            if (factura.getImpIVA() != null && factura.getImpIVA().compareTo(BigDecimal.ZERO) > 0) {
                iva.setIdIvaTipo(5);
                iva.setBaseImp(factura.getImpNeto());
                iva.setImporte(factura.getImpIVA());
            } else {
                iva.setIdIvaTipo(3);
                iva.setBaseImp(factura.getImpNeto());
                iva.setImporte(BigDecimal.ZERO);
            }
            ivas.add(iva);
        }
        factura.setIvas(ivas);
        factura.setTributos(new ArrayList<>());

        return factura;
    }

    private WSFEService.FECAERequest prepararFECAERequest(FacturaAFIP factura) {
        WSFEService.FECAERequest request = new WSFEService.FECAERequest();
        request.setPtoVta(factura.getPtoVta());
        request.setCbteTipo(factura.getCbteTipo());

        WSFEService.FECAERequestItem item = new WSFEService.FECAERequestItem();
        item.setConcepto(factura.getConcepto());
        item.setDocTipo(factura.getClienteAFIP().getDocTipo());
        item.setDocNro(factura.getClienteAFIP().getDocNro());
        item.setCbteDesde(factura.getCbteNro());
        item.setCbteHasta(factura.getCbteNro());
        item.setCbteFch(factura.getCbteFch());
        item.setImpTotal(factura.getImpTotal());
        item.setImpTotConc(factura.getImpTotConc());
        item.setImpNeto(factura.getImpNeto());
        item.setImpOpEx(factura.getImpOpEx());
        item.setImpTrib(factura.getImpTrib());
        item.setImpIVA(factura.getImpIVA());
        item.setMonId(factura.getMonId());
        item.setMonCotiz(factura.getMonCotiz());
        item.setCondicionIVAReceptorId(factura.getClienteAFIP().getCondicionIVAReceptorId());

        if (factura.getIvas() != null) {
            List<WSFEService.FECAERequestIva> ivas = new ArrayList<>();
            for (FacturaIvaAFIP iva : factura.getIvas()) {
                WSFEService.FECAERequestIva ivaReq = new WSFEService.FECAERequestIva();
                ivaReq.setId(iva.getIdIvaTipo());
                ivaReq.setBaseImp(iva.getBaseImp());
                ivaReq.setImporte(iva.getImporte());
                ivas.add(ivaReq);
            }
            item.setIvas(ivas);
        }

        request.getItems().add(item);
        return request;
    }

    private FacturaAFIP cargarFacturaCompleta(Long id) {
        return facturaAFIPRepository.findById(id).map(factura -> {
            adjuntarColeccionesFactura(factura, id);
            return factura;
        }).orElse(null);
    }

    private void adjuntarColeccionesFactura(FacturaAFIP factura, Long id) {
        List<FacturaItemAFIP> items = facturaItemAFIPRepository.findByFacturaAFIP_IdFacturaAFIP(id);
        if (factura.getItems() == null) {
            factura.setItems(new ArrayList<>(items));
        } else {
            factura.getItems().clear();
            factura.getItems().addAll(items);
        }

        List<FacturaIvaAFIP> ivas = facturaIvaAFIPRepository.findByFacturaAFIP_IdFacturaAFIP(id);
        if (factura.getIvas() == null) {
            factura.setIvas(new ArrayList<>(ivas));
        } else {
            factura.getIvas().clear();
            factura.getIvas().addAll(ivas);
        }
    }

    private FacturaAFIPResponse toResponse(FacturaAFIP factura) {
        return FacturaAFIPResponse.builder()
                .id(factura.getIdFacturaAFIP())
                .ventaId(factura.getVenta() != null ? factura.getVenta().getId() : null)
                .cbteTipo(factura.getCbteTipo())
                .tipoComprobante(obtenerTipoComprobante(factura.getCbteTipo()))
                .ptoVta(factura.getPtoVta())
                .cbteNro(factura.getCbteNro())
                .cbteFch(factura.getCbteFch())
                .cae(factura.getCae())
                .caeFchVto(factura.getCaeFchVto())
                .resultado(factura.getResultado())
                .estado(obtenerEstado(factura.getResultado()))
                .motivos(factura.getMotivos())
                .observaciones(factura.getObservaciones())
                .impTotal(factura.getImpTotal())
                .impNeto(factura.getImpNeto())
                .impIVA(factura.getImpIVA())
                .fechaEmision(factura.getFechaEmision())
                .cliente(factura.getClienteAFIP() != null ? FacturaAFIPResponse.ClienteAFIPResponse.builder()
                        .razonSocial(factura.getClienteAFIP().getRazonSocial())
                        .docTipo(factura.getClienteAFIP().getDocTipo())
                        .docNro(factura.getClienteAFIP().getDocNro())
                        .condicionIVAReceptorId(factura.getClienteAFIP().getCondicionIVAReceptorId())
                        .domicilio(factura.getClienteAFIP().getDomicilio())
                        .build() : null)
                .items(factura.getItems() != null ? factura.getItems().stream()
                        .map(item -> FacturaAFIPResponse.ItemResponse.builder()
                                .descripcion(item.getDescripcion())
                                .cantidad(item.getCantidad())
                                .precioUnitario(item.getPrecioUnitario())
                                .subtotal(item.getSubtotal())
                                .codigo(item.getCodigo())
                                .build())
                        .collect(Collectors.toList()) : List.of())
                .build();
    }

    private String obtenerTipoComprobante(Integer tipo) {
        if (tipo == null) {
            return "N/A";
        }
        return switch (tipo) {
            case 1 -> "Factura A";
            case 6 -> "Factura B";
            case 11 -> "Factura C";
            default -> "Tipo " + tipo;
        };
    }

    private String obtenerEstado(String resultado) {
        if (resultado == null) {
            return "N/A";
        }
        return switch (resultado) {
            case "A" -> "Aprobado";
            case "R" -> "Rechazado";
            case "O" -> "Observado";
            case "P" -> "Pendiente";
            default -> resultado;
        };
    }
}
