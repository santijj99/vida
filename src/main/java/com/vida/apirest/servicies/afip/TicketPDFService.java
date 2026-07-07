package com.vida.apirest.servicies.afip;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vida.apirest.servicies.afip.AfipContext;
import com.vida.apirest.servicies.afip.AfipContextHolder;
import com.vida.apirest.model.afip.FacturaAFIP;
import com.vida.apirest.model.afip.FacturaItemAFIP;
import com.vida.apirest.model.afip.FacturaIvaAFIP;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.model.articulo.VarianteArticulo;
import com.vida.apirest.dto.credito.ClienteCreditosResponse;
import com.vida.apirest.dto.credito.CreditoClienteResponse;
import com.vida.apirest.dto.credito.CuotaCreditoResponse;
import com.vida.apirest.model.credito.PagoCuota;
import com.vida.apirest.model.credito.Credito;
import com.vida.apirest.model.credito.Cuota;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.model.venta.PagoVenta;
import com.vida.apirest.model.venta.Venta;
import com.vida.apirest.model.venta.VentaDetalle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class TicketPDFService {

    private static final float ANCHO_TICKET = 226f;
    private static final float ALTO_BASE = 800f;
    private static final float ALTO_POR_ITEM_EXTRA = 20f;
    private static final float ALTO_POR_CUOTA = 18f;
    private static final float ALTO_POR_PAGO = 16f;
    private static final DateTimeFormatter FECHA_VENTA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font FONT_NORMAL = new Font(Font.FontFamily.COURIER, 10, Font.NORMAL);
    private static final Font FONT_BOLD = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);
    private static final Font FONT_LARGE = new Font(Font.FontFamily.COURIER, 16, Font.BOLD);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");

    public record DatosEmpresaTicket(
            String razonSocial,
            String direccion,
            String cuit,
            String condicionIva,
            String iibb,
            String inicioActividad
    ) {
        public static DatosEmpresaTicket from(AfipContext ctx) {
            return new DatosEmpresaTicket(
                    ctx.razonSocial(),
                    ctx.direccion(),
                    ctx.cuit(),
                    ctx.condicionIva(),
                    ctx.iibb(),
                    ctx.inicioActividad()
            );
        }

        public static DatosEmpresaTicket fromEmpresa(Empresa empresa) {
            String razon = empresa.getRazonSocial() != null && !empresa.getRazonSocial().isBlank()
                    ? empresa.getRazonSocial()
                    : empresa.getNombre();
            return new DatosEmpresaTicket(
                    razon != null ? razon : "",
                    empresa.getDomicilio() != null ? empresa.getDomicilio() : "",
                    empresa.getCuit() != null ? empresa.getCuit() : "",
                    "",
                    "",
                    ""
            );
        }
    }

    public static List<Cuota> ordenarCuotas(List<Cuota> cuotas) {
        if (cuotas == null || cuotas.isEmpty()) {
            return List.of();
        }
        return cuotas.stream()
                .sorted(Comparator
                        .comparing(Cuota::getFechaVencimiento, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(c -> c.getNumero() != null ? c.getNumero() : ""))
                .toList();
    }
    
    /**
     * Genera un PDF del ticket de factura AFIP
     * @param facturaAFIP La factura AFIP a imprimir
     * @param outputPath Ruta donde guardar el PDF
     * @throws Exception Si hay error al generar el PDF
     */
    public void generarTicketPDF(FacturaAFIP facturaAFIP, String outputPath) throws Exception {
        byte[] bytes = generarTicketPDFBytes(facturaAFIP, null, null);
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(bytes);
        }
    }
    
    /**
     * Genera el PDF y retorna los bytes
     */
    public byte[] generarTicketPDFBytes(FacturaAFIP facturaAFIP) throws Exception {
        return generarTicketPDFBytes(facturaAFIP, null, null);
    }

    public byte[] generarTicketPDFBytes(FacturaAFIP facturaAFIP, Credito credito, List<Cuota> cuotas) throws Exception {
        int numItems = facturaAFIP.getItems() != null ? facturaAFIP.getItems().size() : 0;
        int numCuotas = cuotas != null ? cuotas.size() : 0;
        Document document = crearDocumento(numItems, numCuotas);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();
        
        try {
            agregarInfoEmpresa(document);
            agregarInfoComprobante(document, facturaAFIP);
            agregarCliente(document, facturaAFIP);
            agregarItems(document, facturaAFIP);
            agregarTotal(document, facturaAFIP);
            if (credito != null && cuotas != null && !cuotas.isEmpty()) {
                agregarResumenCredito(document, credito);
                agregarPlanCuotas(document, cuotas);
            }
            agregarCAE(document, facturaAFIP);
            agregarQRCode(document, facturaAFIP);
            agregarPiePagina(document, true);
        } finally {
            document.close();
        }
        
        return baos.toByteArray();
    }

    public byte[] generarTicketCreditoPersonalBytes(
            Venta venta,
            Credito credito,
            List<Cuota> cuotas,
            DatosEmpresaTicket empresa
    ) throws Exception {
        return generarTicketVentaBytes(venta, empresa, credito, cuotas);
    }

    public byte[] generarTicketVentaBytes(
            Venta venta,
            DatosEmpresaTicket empresa,
            Credito credito,
            List<Cuota> cuotas
    ) throws Exception {
        int numItems = venta.getDetalles() != null ? venta.getDetalles().size() : 0;
        int numCuotas = cuotas != null ? cuotas.size() : 0;
        int numPagos = contarPagosTicket(venta.getPagos());
        Document document = crearDocumento(numItems, numCuotas, numPagos);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        try {
            agregarInfoEmpresa(document, empresa);
            agregarInfoComprobanteVenta(document, venta, credito);
            agregarClienteVenta(document, venta);
            agregarItemsVenta(document, venta);
            agregarTotalVenta(document, venta);
            agregarPagosVenta(document, venta.getPagos(), credito);
            if (credito != null) {
                agregarResumenCredito(document, credito);
            }
            if (cuotas != null && !cuotas.isEmpty()) {
                agregarPlanCuotas(document, cuotas);
            }
            agregarPiePagina(document, false);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private int contarPagosTicket(Collection<PagoVenta> pagos) {
        if (pagos == null || pagos.isEmpty()) {
            return 0;
        }
        return (int) pagos.stream()
                .filter(p -> p.getMonto() != null && p.getMonto().compareTo(BigDecimal.ZERO) > 0)
                .count();
    }

    private Document crearDocumento(int numItems, int numCuotas, int numPagos) {
        float altoTicket = ALTO_BASE
                + Math.max(0, numItems - 3) * ALTO_POR_ITEM_EXTRA
                + numCuotas * ALTO_POR_CUOTA
                + Math.max(0, numPagos) * ALTO_POR_PAGO;
        Document document = new Document(new Rectangle(ANCHO_TICKET, altoTicket));
        document.setMargins(15, 15, 15, 15);
        return document;
    }

    private Document crearDocumento(int numItems, int numCuotas) {
        return crearDocumento(numItems, numCuotas, 0);
    }
    
    private void agregarInfoEmpresa(Document document) throws DocumentException {
        AfipContext empresa = AfipContextHolder.require();
        agregarInfoEmpresa(document, DatosEmpresaTicket.from(empresa));
    }

    private void agregarInfoEmpresa(Document document, DatosEmpresaTicket empresa) throws DocumentException {
        Paragraph p = new Paragraph();
        p.setFont(FONT_NORMAL);
        p.add(new Chunk("Razón social: " + empresa.razonSocial() + "\n", FONT_NORMAL));
        p.add(new Chunk("Direccion: " + empresa.direccion() + "\n", FONT_NORMAL));
        p.add(new Chunk("C.U.I.T.: " + empresa.cuit() + "\n", FONT_NORMAL));
        if (empresa.condicionIva() != null && !empresa.condicionIva().isBlank()) {
            p.add(new Chunk(empresa.condicionIva() + "\n", FONT_NORMAL));
        }
        if (empresa.iibb() != null && !empresa.iibb().isBlank()) {
            p.add(new Chunk("IIBB: " + empresa.iibb() + "\n", FONT_NORMAL));
        }
        if (empresa.inicioActividad() != null && !empresa.inicioActividad().isBlank()) {
            p.add(new Chunk("Inicio de actividad: " + empresa.inicioActividad() + "\n", FONT_NORMAL));
        }
        document.add(p);
        document.add(new Paragraph(" "));
    }

    private void agregarInfoComprobanteVenta(Document document, Venta venta, Credito credito) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));

        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk("COMPROBANTE\n", FONT_LARGE));
        p.add(new Chunk(descripcionTipoVenta(venta, credito) + "\n", FONT_NORMAL));
        p.add(new Chunk("Venta: " + (venta.getNumeroFactura() != null ? venta.getNumeroFactura() : "-") + "\n", FONT_NORMAL));
        if (venta.getFechaVenta() != null) {
            p.add(new Chunk("Fecha: " + venta.getFechaVenta().format(FECHA_VENTA_FMT) + "\n", FONT_NORMAL));
        }
        if (credito != null && credito.getNumero() != null) {
            p.add(new Chunk("Crédito: " + credito.getNumero() + "\n", FONT_NORMAL));
        }
        document.add(p);
        document.add(new Paragraph(" "));
    }

    private String descripcionTipoVenta(Venta venta, Credito credito) {
        if (credito != null) {
            return "Crédito personal";
        }
        if (venta.getPagos() != null && venta.getPagos().size() > 1) {
            return "Pagos múltiples";
        }
        String metodo = venta.getMetodoPago();
        if (metodo == null || metodo.isBlank()) {
            return "Venta";
        }
        return etiquetaMetodoPago(metodo);
    }

    private void agregarPagosVenta(Document document, Collection<PagoVenta> pagos, Credito credito) throws DocumentException {
        if (pagos == null || pagos.isEmpty()) {
            return;
        }

        List<PagoVenta> pagosVisibles = pagos.stream()
                .filter(p -> p.getMonto() != null && p.getMonto().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        if (pagosVisibles.isEmpty() || (pagosVisibles.size() <= 1 && credito == null)) {
            return;
        }

        document.add(new LineSeparator());
        document.add(new Paragraph(" "));

        Paragraph titulo = new Paragraph(
                pagosVisibles.size() > 1 ? "FORMAS DE PAGO" : "FORMA DE PAGO",
                FONT_BOLD
        );
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 2});

        for (PagoVenta pago : pagosVisibles) {
            agregarCelda(table, etiquetaMetodoPago(pago.getMetodoPago()), FONT_NORMAL, Element.ALIGN_LEFT);
            agregarCelda(table, "$ " + DECIMAL_FORMAT.format(pago.getMonto()), FONT_NORMAL, Element.ALIGN_RIGHT);
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private String etiquetaMetodoPago(String metodo) {
        if (metodo == null || metodo.isBlank()) {
            return "Pago";
        }
        return switch (metodo.trim().toUpperCase()) {
            case "EFECTIVO" -> "Efectivo";
            case "TRANSFERENCIA" -> "Transferencia";
            case "CREDITO" -> "Crédito";
            case "COMBINADO" -> "Pagos múltiples";
            case "QR" -> "QR";
            case "TARJETA DE CREDITO", "TARJETA CREDITO" -> "Tarjeta crédito";
            case "TARJETA DE DEBITO", "TARJETA DEBITO" -> "Tarjeta débito";
            default -> metodo;
        };
    }

    private void agregarClienteVenta(Document document, Venta venta) throws DocumentException {
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));

        Paragraph p = new Paragraph();
        Cliente cliente = venta.getCliente();
        if (cliente != null) {
            String nombre = ((cliente.getNombre() != null ? cliente.getNombre() : "")
                    + " " + (cliente.getApellido() != null ? cliente.getApellido() : "")).trim();
            if (!nombre.isBlank()) {
                p.add(new Chunk(nombre + "\n", FONT_NORMAL));
            }
            if (cliente.getDni() != null && !cliente.getDni().isBlank()) {
                p.add(new Chunk("DNI: " + cliente.getDni() + "\n", FONT_NORMAL));
            }
        } else {
            p.add(new Chunk("A CONSUMIDOR FINAL\n", FONT_NORMAL));
        }
        document.add(p);
        document.add(new Paragraph(" "));
    }

    private void agregarItemsVenta(Document document, Venta venta) throws DocumentException {
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 5, 1, 2});

        agregarCelda(table, "Cant.", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, "Descripción", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, "IVA", FONT_BOLD, Element.ALIGN_CENTER);
        agregarCelda(table, "Precio", FONT_BOLD, Element.ALIGN_RIGHT);

        if (venta.getDetalles() != null) {
            for (VentaDetalle detalle : venta.getDetalles()) {
                agregarCelda(table, String.valueOf(detalle.getCantidad()), FONT_NORMAL, Element.ALIGN_LEFT);
                agregarCelda(table, descripcionDetalle(detalle), FONT_NORMAL, Element.ALIGN_LEFT);
                agregarCelda(table, obtenerIVADetalle(detalle), FONT_NORMAL, Element.ALIGN_CENTER);
                BigDecimal precio = detalle.getTotal() != null ? detalle.getTotal() : detalle.getSubtotal();
                agregarCelda(table, DECIMAL_FORMAT.format(precio != null ? precio : BigDecimal.ZERO), FONT_NORMAL, Element.ALIGN_RIGHT);
            }
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void agregarTotalVenta(Document document, Venta venta) throws DocumentException {
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5, 2});

        agregarCelda(table, "TOTAL", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, DECIMAL_FORMAT.format(venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO), FONT_BOLD, Element.ALIGN_RIGHT);

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void agregarResumenCredito(Document document, Credito credito) throws DocumentException {
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));

        Paragraph p = new Paragraph();
        p.setFont(FONT_NORMAL);
        p.add(new Chunk("Financiado: $" + DECIMAL_FORMAT.format(credito.getImporte()) + "\n", FONT_BOLD));
        if (credito.getSaldo() != null) {
            p.add(new Chunk("Saldo: $" + DECIMAL_FORMAT.format(credito.getSaldo()) + "\n", FONT_NORMAL));
        }
        if (credito.getPlazoMeses() != null) {
            p.add(new Chunk("Plazo: " + credito.getPlazoMeses() + " cuotas\n", FONT_NORMAL));
        }
        if (credito.getTasaInteres() != null && credito.getTasaInteres().compareTo(BigDecimal.ZERO) > 0) {
            p.add(new Chunk("Tasa: " + credito.getTasaInteres().stripTrailingZeros().toPlainString() + "%\n", FONT_NORMAL));
        }
        document.add(p);
        document.add(new Paragraph(" "));
    }

    private void agregarPlanCuotas(Document document, List<Cuota> cuotas) throws DocumentException {
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));

        Paragraph titulo = new Paragraph("DETALLE DE CUOTAS", FONT_BOLD);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2, 2, 2});

        agregarCelda(table, "N°", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, "Venc.", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, "Monto", FONT_BOLD, Element.ALIGN_RIGHT);
        agregarCelda(table, "Estado", FONT_BOLD, Element.ALIGN_CENTER);

        for (Cuota cuota : cuotas) {
            agregarCelda(table, cuota.getNumero() != null ? cuota.getNumero() : "-", FONT_NORMAL, Element.ALIGN_LEFT);
            agregarCelda(table, formatearFechaCuota(cuota.getFechaVencimiento()), FONT_NORMAL, Element.ALIGN_LEFT);
            agregarCelda(table, DECIMAL_FORMAT.format(cuota.getMonto() != null ? cuota.getMonto() : BigDecimal.ZERO), FONT_NORMAL, Element.ALIGN_RIGHT);
            agregarCelda(table, etiquetaEstadoCuota(cuota.getEstado()), FONT_NORMAL, Element.ALIGN_CENTER);
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private String descripcionDetalle(VentaDetalle detalle) {
        StringBuilder sb = new StringBuilder();
        Articulo articulo = detalle.getArticulo();
        if (articulo != null) {
            if (articulo.getModelo() != null && !articulo.getModelo().isBlank()) {
                sb.append(articulo.getModelo());
            } else if (articulo.getDescripcion() != null && !articulo.getDescripcion().isBlank()) {
                sb.append(articulo.getDescripcion());
            } else if (articulo.getCodigo() != null) {
                sb.append(articulo.getCodigo());
            }
        }
        VarianteArticulo variante = detalle.getVariante();
        if (variante != null) {
            if (variante.getTalle() != null && variante.getTalle().getNumero() != null) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append(variante.getTalle().getNumero());
            }
            if (variante.getColor() != null && variante.getColor().getNombre() != null) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append(variante.getColor().getNombre());
            }
        }
        return !sb.isEmpty() ? sb.toString() : "Artículo";
    }

    private String obtenerIVADetalle(VentaDetalle detalle) {
        if (detalle.getImpuesto() != null
                && detalle.getImpuesto().compareTo(BigDecimal.ZERO) > 0
                && detalle.getSubtotal() != null
                && detalle.getSubtotal().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pct = detalle.getImpuesto()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(detalle.getSubtotal(), 0, RoundingMode.HALF_UP);
            return pct.toPlainString() + "%";
        }
        return "0%";
    }

    private String formatearFechaCuota(LocalDateTime fecha) {
        if (fecha == null) {
            return "-";
        }
        return String.format("%02d/%02d/%04d", fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getYear());
    }

    private String etiquetaEstadoCuota(Cuota.EstadoCuota estado) {
        if (estado == null) {
            return "-";
        }
        return switch (estado) {
            case PENDIENTE -> "Pend.";
            case PAGADA -> "Pagada";
            case VENCIDA -> "Vencida";
            case CANCELADA -> "Cancel.";
            case ELIMINADA -> "Elim.";
        };
    }
    
    private void agregarInfoComprobante(Document document, FacturaAFIP factura) throws DocumentException {
        // Línea separadora
        document.add(new Paragraph(" "));
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));
        
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        
        // Tipo de factura
        String tipoFactura = obtenerTipoFactura(factura.getCbteTipo());
        p.add(new Chunk(tipoFactura + "\n", FONT_LARGE));
        p.add(new Chunk("Codigo " + factura.getCbteTipo() + "\n", FONT_NORMAL));
        
        // Punto de venta y número
        String ptoVtaStr = String.format("%05d", factura.getPtoVta());
        String cbteNroStr = String.format("%08d", factura.getCbteNro());
        p.add(new Chunk("P.V: " + ptoVtaStr + "\n", FONT_NORMAL));
        p.add(new Chunk("Nro: " + cbteNroStr + "\n", FONT_NORMAL));
        
        // Fecha
        String fecha = formatearFecha(factura.getCbteFch());
        p.add(new Chunk("Fecha: " + fecha + "\n", FONT_NORMAL));
        
        // Concepto
        String concepto = obtenerConcepto(factura.getConcepto());
        p.add(new Chunk("Concepto: " + concepto + "\n", FONT_NORMAL));
        
        document.add(p);
        document.add(new Paragraph(" "));
    }
    
    private void agregarCliente(Document document, FacturaAFIP factura) throws DocumentException {
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));
        
        Paragraph p = new Paragraph();
        if (factura.getClienteAFIP() != null && factura.getClienteAFIP().getRazonSocial() != null) {
            p.add(new Chunk(factura.getClienteAFIP().getRazonSocial() + "\n", FONT_NORMAL));
        } else {
            p.add(new Chunk("A CONSUMIDOR FINAL\n", FONT_NORMAL));
        }
        document.add(p);
        document.add(new Paragraph(" "));
    }
    
    private void agregarItems(Document document, FacturaAFIP factura) throws DocumentException {
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));
        
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 5, 1, 2});
        
        // Headers
        agregarCelda(table, "Cant.", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, "Descripción", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, "IVA", FONT_BOLD, Element.ALIGN_CENTER);
        agregarCelda(table, "Precio", FONT_BOLD, Element.ALIGN_RIGHT);
        
        // Items
        if (factura.getItems() != null) {
            for (FacturaItemAFIP item : factura.getItems()) {
                agregarCelda(table, DECIMAL_FORMAT.format(item.getCantidad()), FONT_NORMAL, Element.ALIGN_LEFT);
                agregarCelda(table, item.getDescripcion(), FONT_NORMAL, Element.ALIGN_LEFT);
                
                // Obtener IVA del item
                String ivaStr = obtenerIVAItem(factura, item);
                agregarCelda(table, ivaStr, FONT_NORMAL, Element.ALIGN_CENTER);
                
                agregarCelda(table, DECIMAL_FORMAT.format(item.getSubtotal()), FONT_NORMAL, Element.ALIGN_RIGHT);
            }
        }
        
        document.add(table);
        document.add(new Paragraph(" "));
    }
    
    private void agregarTotal(Document document, FacturaAFIP factura) throws DocumentException {
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));
        
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5, 2});
        
        agregarCelda(table, "TOTAL", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, DECIMAL_FORMAT.format(factura.getImpTotal()), FONT_BOLD, Element.ALIGN_RIGHT);
        
        document.add(table);
        document.add(new Paragraph(" "));
    }
    
    private void agregarCAE(Document document, FacturaAFIP factura) throws DocumentException {
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));
        
        Paragraph p = new Paragraph();
        if (factura.getCae() != null && !factura.getCae().isEmpty()) {
            p.add(new Chunk("CAE: " + factura.getCae() + "\n", FONT_NORMAL));
        }
        if (factura.getCaeFchVto() != null && !factura.getCaeFchVto().isEmpty()) {
            String fechaVto = formatearFecha(factura.getCaeFchVto());
            p.add(new Chunk("Vto: " + fechaVto + "\n", FONT_NORMAL));
        }
        document.add(p);
    }
    
    private void agregarQRCode(Document document, FacturaAFIP factura) throws DocumentException, IOException, WriterException {
        document.add(new Paragraph(" "));
        
        // Generar código QR con datos de AFIP
        String qrData = generarDatosQR(factura);
        BufferedImage qrImage = generarQRCode(qrData, 200, 200);
        
        // Convertir BufferedImage a Image de iText
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", baos);
        Image qrCodeImage = Image.getInstance(baos.toByteArray());
        qrCodeImage.setAlignment(Element.ALIGN_CENTER);
        qrCodeImage.scaleToFit(150, 150);
        
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(qrCodeImage);
        document.add(p);
    }
    
    
    private void agregarCelda(PdfPTable table, String texto, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(2);
        table.addCell(cell);
    }
    
    private String obtenerTipoFactura(Integer cbteTipo) {
        switch (cbteTipo) {
            case 1: return "FACTURA A";
            case 6: return "FACTURA B";
            case 11: return "FACTURA C";
            default: return "FACTURA " + cbteTipo;
        }
    }
    
    private String obtenerConcepto(Integer concepto) {
        switch (concepto) {
            case 1: return "Productos";
            case 2: return "Servicios";
            case 3: return "Productos y Servicios";
            default: return "Concepto " + concepto;
        }
    }
    
    private String formatearFecha(String fechaYYYYMMDD) {
        try {
            if (fechaYYYYMMDD == null || fechaYYYYMMDD.length() != 8) {
                return fechaYYYYMMDD;
            }
            String anio = fechaYYYYMMDD.substring(0, 4);
            String mes = fechaYYYYMMDD.substring(4, 6);
            String dia = fechaYYYYMMDD.substring(6, 8);
            return dia + "/" + mes + "/" + anio;
        } catch (Exception e) {
            return fechaYYYYMMDD;
        }
    }
    
    private String obtenerIVAItem(FacturaAFIP factura, FacturaItemAFIP item) {
        if (factura.getIvas() == null || factura.getIvas().isEmpty()) {
            return "0%";
        }
        
        // Buscar el IVA correspondiente al item
        // Por simplicidad, usamos el primer IVA de la factura
        FacturaIvaAFIP iva = factura.getIvas().get(0);
        Integer idIva = iva.getIdIvaTipo();
        
        // Mapear ID de IVA a porcentaje
        switch (idIva) {
            case 3: return "0%"; // Exento
            case 4: return "10.5%";
            case 5: return "21%";
            case 6: return "27%";
            default: return idIva.toString() + "%";
        }
    }
    
    /**
     * Genera la URL para el código QR según la especificación oficial de ARCA
     * Formato: https://www.arca.gob.ar/fe/qr/?p={JSON_BASE64}
     * El JSON contiene los datos del comprobante según especificación ARCA
     */
    private String generarDatosQR(FacturaAFIP factura) {
        try {
            // Construir JSON según especificación ARCA
            StringBuilder json = new StringBuilder();
            json.append("{");
            
            // ver: versión del formato (1)
            json.append("\"ver\":1,");
            
            // fecha: formato RFC3339 full-date (YYYY-MM-DD)
            String fecha = formatearFechaParaJSON(factura.getCbteFch());
            json.append("\"fecha\":\"").append(fecha).append("\",");
            
            // cuit: CUIT del emisor (11 dígitos)
            String cuit = AfipContextHolder.require().cuitSinGuiones();
            if (cuit != null && !cuit.isBlank()) {
                json.append("\"cuit\":").append(cuit).append(",");
            } else {
                throw new Exception("CUIT no configurado en la empresa");
            }
            
            // ptoVta: Punto de venta (hasta 5 dígitos)
            if (factura.getPtoVta() != null) {
                json.append("\"ptoVta\":").append(factura.getPtoVta()).append(",");
            } else {
                throw new Exception("Punto de venta no especificado");
            }
            
            // tipoCmp: Tipo de comprobante (hasta 3 dígitos)
            if (factura.getCbteTipo() != null) {
                json.append("\"tipoCmp\":").append(factura.getCbteTipo()).append(",");
            } else {
                throw new Exception("Tipo de comprobante no especificado");
            }
            
            // nroCmp: Número de comprobante (hasta 8 dígitos)
            if (factura.getCbteNro() != null) {
                json.append("\"nroCmp\":").append(factura.getCbteNro()).append(",");
            } else {
                throw new Exception("Número de comprobante no especificado");
            }
            
            // importe: Importe total (hasta 13 enteros y 2 decimales)
            if (factura.getImpTotal() != null) {
                // Formatear sin separadores, usar punto como decimal
                String importe = factura.getImpTotal().toString().replace(",", ".");
                json.append("\"importe\":").append(importe).append(",");
            } else {
                throw new Exception("Importe total no especificado");
            }
            
            // moneda: Código de moneda (3 caracteres)
            String moneda = factura.getMonId() != null ? factura.getMonId() : "PES";
            json.append("\"moneda\":\"").append(moneda).append("\",");
            
            // ctz: Cotización (hasta 13 enteros y 6 decimales)
            BigDecimal cotizacion = factura.getMonCotiz() != null ? factura.getMonCotiz() : BigDecimal.ONE;
            String ctz = cotizacion.toString().replace(",", ".");
            json.append("\"ctz\":").append(ctz).append(",");
            
            // tipoDocRec y nroDocRec: DE CORRESPONDER
            if (factura.getClienteAFIP() != null) {
                Integer docTipo = factura.getClienteAFIP().getDocTipo();
                String docNro = factura.getClienteAFIP().getDocNro();
                
                // Solo incluir si no es "Sin identificar" (99) o "0"
                if (docTipo != null && docTipo != 99 && docNro != null && !docNro.isEmpty() && !docNro.equals("0")) {
                    json.append("\"tipoDocRec\":").append(docTipo).append(",");
                    json.append("\"nroDocRec\":").append(docNro).append(",");
                }
            }
            
            // tipoCodAut: "A" para CAEA, "E" para CAE
            json.append("\"tipoCodAut\":\"E\",");
            
            // codAut: Código de autorización (14 dígitos)
            String cae = factura.getCae();
            if (cae != null && !cae.isEmpty()) {
                // Asegurar que tenga 14 dígitos
                cae = cae.trim();
                json.append("\"codAut\":").append(cae);
            } else {
                throw new Exception("CAE no especificado");
            }
            
            json.append("}");
            
            // Codificar JSON en Base64
            String jsonString = json.toString();
            byte[] jsonBytes = jsonString.getBytes("UTF-8");
            String jsonBase64 = java.util.Base64.getEncoder().encodeToString(jsonBytes);
            
            // Construir URL según especificación ARCA
            return "https://www.arca.gob.ar/fe/qr/?p=" + jsonBase64;
            
        } catch (Exception e) {
            System.err.println("Error al generar QR: " + e.getMessage());
            e.printStackTrace();
            // Si hay error, devolver URL base sin parámetros
            return "https://www.arca.gob.ar/fe/qr/";
        }
    }
    
    /**
     * Codifica un string para URL
     */
    private String encodeURL(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
    
    /**
     * Formatea la fecha para el JSON (RFC3339 full-date: YYYY-MM-DD)
     */
    private String formatearFechaParaJSON(String fechaYYYYMMDD) {
        try {
            if (fechaYYYYMMDD == null || fechaYYYYMMDD.length() != 8) {
                throw new Exception("Formato de fecha inválido: " + fechaYYYYMMDD);
            }
            String anio = fechaYYYYMMDD.substring(0, 4);
            String mes = fechaYYYYMMDD.substring(4, 6);
            String dia = fechaYYYYMMDD.substring(6, 8);
            return anio + "-" + mes + "-" + dia;
        } catch (Exception e) {
            // Si falla, usar fecha actual
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format(new Date());
        }
    }
    
    /**
     * Genera un código QR como BufferedImage
     */
    private BufferedImage generarQRCode(String data, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
    
    private void agregarPiePagina(Document document, boolean facturaFiscal) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));
        
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.setFont(FONT_NORMAL);
        p.add(new Chunk("Gracias por su compra\n", FONT_NORMAL));
        if (facturaFiscal) {
            p.add(new Chunk("Comprobante válido como factura\n", FONT_NORMAL));
        }
        document.add(p);
    }

    public byte[] generarTicketPagoCuotasBytes(List<PagoCuota> pagos, DatosEmpresaTicket empresa) throws Exception {
        if (pagos == null || pagos.isEmpty()) {
            throw new IllegalArgumentException("No hay pagos para imprimir");
        }

        PagoCuota primerPago = pagos.get(0);
        var credito = primerPago.getCuota().getCredito();
        var cliente = credito.getCliente();
        int lineas = pagos.size() + 8;
        Document document = crearDocumento(0, 0, lineas);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        try {
            agregarInfoEmpresa(document, empresa);
            agregarEncabezadoCobroCuotas(document, cliente, primerPago);
            agregarDetallePagosCuotas(document, pagos);
            agregarTotalesCobroCuotas(document, pagos);
            agregarPiePagina(document, false);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    public byte[] generarResumenCuentaCreditoBytes(ClienteCreditosResponse cuenta, DatosEmpresaTicket empresa) throws Exception {
        int cuotasActivas = cuenta.getCreditosActivos() != null
                ? cuenta.getCreditosActivos().stream().mapToInt(c -> c.getCuotas() != null ? c.getCuotas().size() : 0).sum()
                : 0;
        int lineas = cuotasActivas + (cuenta.getCreditosActivos() != null ? cuenta.getCreditosActivos().size() * 3 : 0) + 12;
        Document document = crearDocumento(0, 0, lineas);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        try {
            agregarInfoEmpresa(document, empresa);
            agregarEncabezadoResumenCuenta(document, cuenta);
            agregarResumenMontosCuenta(document, cuenta);
            if (cuenta.getCreditosActivos() != null) {
                for (CreditoClienteResponse credito : cuenta.getCreditosActivos()) {
                    agregarCreditoEnResumen(document, credito);
                }
            }
            agregarPiePagina(document, false);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private void agregarEncabezadoCobroCuotas(
            Document document,
            com.vida.apirest.model.persona.Cliente cliente,
            PagoCuota primerPago
    ) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));

        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk("COMPROBANTE\n", FONT_LARGE));
        p.add(new Chunk("Cobro de cuotas\n", FONT_NORMAL));
        if (primerPago.getCreatedAt() != null) {
            p.add(new Chunk("Fecha: " + primerPago.getCreatedAt().format(FECHA_VENTA_FMT) + "\n", FONT_NORMAL));
        }
        document.add(p);
        document.add(new Paragraph(" "));

        document.add(new LineSeparator());
        document.add(new Paragraph(" "));
        Paragraph clienteP = new Paragraph();
        if (cliente != null) {
            String nombre = ((cliente.getNombre() != null ? cliente.getNombre() : "")
                    + " " + (cliente.getApellido() != null ? cliente.getApellido() : "")).trim();
            if (!nombre.isBlank()) {
                clienteP.add(new Chunk(nombre + "\n", FONT_NORMAL));
            }
            if (cliente.getDni() != null && !cliente.getDni().isBlank()) {
                clienteP.add(new Chunk("DNI: " + cliente.getDni() + "\n", FONT_NORMAL));
            }
        }
        document.add(clienteP);
        document.add(new Paragraph(" "));
    }

    private void agregarDetallePagosCuotas(Document document, List<PagoCuota> pagos) throws DocumentException {
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));

        Paragraph titulo = new Paragraph("DETALLE DEL COBRO", FONT_BOLD);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2, 1, 2, 2});

        agregarCelda(table, "Crédito", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, "Cuota", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, "Pagado", FONT_BOLD, Element.ALIGN_RIGHT);
        agregarCelda(table, "Saldo", FONT_BOLD, Element.ALIGN_RIGHT);

        for (PagoCuota pago : pagos) {
            Cuota cuota = pago.getCuota();
            Credito credito = cuota.getCredito();
            BigDecimal saldoRestante = cuota.getSaldo() != null ? cuota.getSaldo() : BigDecimal.ZERO;
            agregarCelda(table, credito.getNumero() != null ? credito.getNumero() : "-", FONT_NORMAL, Element.ALIGN_LEFT);
            agregarCelda(table, cuota.getNumero() != null ? cuota.getNumero() : "-", FONT_NORMAL, Element.ALIGN_LEFT);
            agregarCelda(table, "$ " + DECIMAL_FORMAT.format(pago.getMonto()), FONT_NORMAL, Element.ALIGN_RIGHT);
            agregarCelda(table, "$ " + DECIMAL_FORMAT.format(saldoRestante), FONT_NORMAL, Element.ALIGN_RIGHT);
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void agregarTotalesCobroCuotas(Document document, List<PagoCuota> pagos) throws DocumentException {
        BigDecimal totalCobrado = pagos.stream()
                .map(PagoCuota::getMonto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String metodo = pagos.get(0).getMetodoPago() != null ? etiquetaMetodoPago(pagos.get(0).getMetodoPago()) : "Pago";

        document.add(new LineSeparator());
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 2});
        agregarCelda(table, "Método", FONT_NORMAL, Element.ALIGN_LEFT);
        agregarCelda(table, metodo, FONT_NORMAL, Element.ALIGN_RIGHT);
        agregarCelda(table, "TOTAL COBRADO", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, "$ " + DECIMAL_FORMAT.format(totalCobrado), FONT_BOLD, Element.ALIGN_RIGHT);
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void agregarEncabezadoResumenCuenta(Document document, ClienteCreditosResponse cuenta) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));

        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk("RESUMEN DE CUENTA\n", FONT_LARGE));
        p.add(new Chunk("Cuenta: " + (cuenta.getCuentaNumero() != null ? cuenta.getCuentaNumero() : "-") + "\n", FONT_NORMAL));
        p.add(new Chunk("Fecha: " + LocalDateTime.now().format(FECHA_VENTA_FMT) + "\n", FONT_NORMAL));
        document.add(p);
        document.add(new Paragraph(" "));

        Paragraph clienteP = new Paragraph();
        String nombre = ((cuenta.getClienteNombre() != null ? cuenta.getClienteNombre() : "")
                + " " + (cuenta.getClienteApellido() != null ? cuenta.getClienteApellido() : "")).trim();
        if (!nombre.isBlank()) {
            clienteP.add(new Chunk(nombre + "\n", FONT_NORMAL));
        }
        if (cuenta.getClienteDni() != null && !cuenta.getClienteDni().isBlank()) {
            clienteP.add(new Chunk("DNI: " + cuenta.getClienteDni() + "\n", FONT_NORMAL));
        }
        document.add(clienteP);
        document.add(new Paragraph(" "));
    }

    private void agregarResumenMontosCuenta(Document document, ClienteCreditosResponse cuenta) throws DocumentException {
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 2});
        agregarCelda(table, "Saldo pendiente", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, "$ " + DECIMAL_FORMAT.format(valorSeguro(cuenta.getSaldoCuenta())), FONT_BOLD, Element.ALIGN_RIGHT);
        agregarCelda(table, "Créditos activos", FONT_NORMAL, Element.ALIGN_LEFT);
        agregarCelda(table, String.valueOf(cuenta.getCantidadCreditos() != null ? cuenta.getCantidadCreditos() : 0), FONT_NORMAL, Element.ALIGN_RIGHT);
        agregarCelda(table, "Total créditos", FONT_NORMAL, Element.ALIGN_LEFT);
        agregarCelda(table, "$ " + DECIMAL_FORMAT.format(valorSeguro(cuenta.getTotalCreditosSacados())), FONT_NORMAL, Element.ALIGN_RIGHT);
        agregarCelda(table, "Total pagado", FONT_NORMAL, Element.ALIGN_LEFT);
        agregarCelda(table, "$ " + DECIMAL_FORMAT.format(valorSeguro(cuenta.getTotalPagado())), FONT_NORMAL, Element.ALIGN_RIGHT);
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void agregarCreditoEnResumen(Document document, CreditoClienteResponse credito) throws DocumentException {
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));

        Paragraph titulo = new Paragraph(
                "Crédito " + (credito.getNumero() != null ? credito.getNumero() : "-")
                        + " · Saldo $ " + DECIMAL_FORMAT.format(valorSeguro(credito.getSaldo())),
                FONT_BOLD
        );
        document.add(titulo);
        document.add(new Paragraph(" "));

        if (credito.getCuotas() == null || credito.getCuotas().isEmpty()) {
            return;
        }

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2, 2, 2});

        agregarCelda(table, "N°", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, "Venc.", FONT_BOLD, Element.ALIGN_LEFT);
        agregarCelda(table, "Saldo", FONT_BOLD, Element.ALIGN_RIGHT);
        agregarCelda(table, "Estado", FONT_BOLD, Element.ALIGN_CENTER);

        for (CuotaCreditoResponse cuota : credito.getCuotas()) {
            agregarCelda(table, cuota.getNumero() != null ? cuota.getNumero() : "-", FONT_NORMAL, Element.ALIGN_LEFT);
            agregarCelda(table, formatearFechaCuota(cuota.getFechaVencimiento()), FONT_NORMAL, Element.ALIGN_LEFT);
            agregarCelda(table, "$ " + DECIMAL_FORMAT.format(valorSeguro(cuota.getSaldo())), FONT_NORMAL, Element.ALIGN_RIGHT);
            agregarCelda(table, cuota.getEstado() != null ? cuota.getEstado() : "-", FONT_NORMAL, Element.ALIGN_CENTER);
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}

