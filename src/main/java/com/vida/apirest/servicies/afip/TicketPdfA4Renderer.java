package com.vida.apirest.servicies.afip;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.vida.apirest.dto.credito.ClienteCreditosResponse;
import com.vida.apirest.dto.credito.CreditoClienteResponse;
import com.vida.apirest.dto.credito.CuotaCreditoResponse;
import com.vida.apirest.model.afip.ClienteAFIP;
import com.vida.apirest.model.afip.FacturaAFIP;
import com.vida.apirest.model.afip.FacturaItemAFIP;
import com.vida.apirest.model.credito.Credito;
import com.vida.apirest.model.credito.Cuota;
import com.vida.apirest.model.credito.PagoCuota;
import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.model.venta.PagoVenta;
import com.vida.apirest.model.venta.Venta;
import com.vida.apirest.model.venta.VentaDetalle;
import com.vida.apirest.servicies.VentaDetalleSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Component
public class TicketPdfA4Renderer {

    private static final float MARGEN = 36f;
    private static final String[] COPIAS_FISCALES = {"ORIGINAL", "DUPLICADO", "TRIPLICADO"};
    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA_CREDITO_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final Font F_NORMAL = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
    private static final Font F_BOLD = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
    private static final Font F_SMALL = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL);
    private static final Font F_SMALL_ITALIC = new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC);
    private static final Font F_EMPRESA = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font F_FACTURA = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font F_LETRA = new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD);
    private static final Font F_ARCA = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private static final BaseColor GRIS_HEADER = new BaseColor(210, 210, 210);

    private static final DecimalFormat MONEDA_FMT = crearFormatoMoneda();

    public byte[] generarFacturaAfip(FacturaAFIP factura, Credito credito, List<Cuota> cuotas) throws Exception {
        TicketPDFService.DatosEmpresaTicket empresa = TicketPDFService.DatosEmpresaTicket.from(AfipContextHolder.require());
        Document document = nuevoDocumentoA4();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();
        try {
            List<Cuota> cuotasOrdenadas = TicketPDFService.ordenarCuotas(cuotas);
            for (int i = 0; i < COPIAS_FISCALES.length; i++) {
                if (i > 0) {
                    document.newPage();
                }
                renderizarFacturaFiscal(document, factura, empresa, COPIAS_FISCALES[i], credito, cuotasOrdenadas);
            }
        } finally {
            document.close();
        }
        return baos.toByteArray();
    }

    public byte[] generarComprobanteVenta(
            Venta venta,
            TicketPDFService.DatosEmpresaTicket empresa,
            Credito credito,
            List<Cuota> cuotas
    ) throws Exception {
        if (credito != null) {
            return generarComprobanteCredito(venta, empresa, credito, cuotas);
        }
        Document document = nuevoDocumentoA4();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();
        try {
            renderizarComprobanteVenta(document, venta, empresa, null, TicketPDFService.ordenarCuotas(cuotas));
        } finally {
            document.close();
        }
        return baos.toByteArray();
    }

    /**
     * Comprobante de crédito con dos copias (ORIGINAL / DUPLICADO) y líneas para firma, aclaración y DNI.
     */
    public byte[] generarComprobanteCredito(
            Venta venta,
            TicketPDFService.DatosEmpresaTicket empresa,
            Credito credito,
            List<Cuota> cuotas
    ) throws Exception {
        Document document = nuevoDocumentoA4();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();
        try {
            List<Cuota> ordenadas = TicketPDFService.ordenarCuotas(cuotas);
            PdfPTable copiaCliente = new PdfPTable(1);
            copiaCliente.setWidthPercentage(100);
            copiaCliente.addCell(celdaCopiaCredito(
                    venta, empresa, credito, ordenadas, true));
            document.add(copiaCliente);

            document.newPage();

            PdfPTable copiaEmpresa = new PdfPTable(1);
            copiaEmpresa.setWidthPercentage(100);
            copiaEmpresa.addCell(celdaCopiaCredito(
                    venta, empresa, credito, ordenadas, false));
            document.add(copiaEmpresa);
        } finally {
            document.close();
        }
        return baos.toByteArray();
    }

    private PdfPCell celdaCopiaCredito(
            Venta venta,
            TicketPDFService.DatosEmpresaTicket empresa,
            Credito credito,
            List<Cuota> cuotas,
            boolean incluirFirma
    ) throws DocumentException {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BaseColor.LIGHT_GRAY);
        cell.setPadding(10);
        cell.setVerticalAlignment(Element.ALIGN_TOP);

        Paragraph empresaInfo = new Paragraph();
        empresaInfo.add(new Chunk(nvl(empresa.razonSocial(), "EMPRESA") + "\n", F_EMPRESA));
        if (empresa.mostrarDireccion()
                && empresa.direccion() != null && !empresa.direccion().isBlank()) {
            empresaInfo.add(new Chunk("Direccion: " + empresa.direccion() + "\n", F_NORMAL));
        }
        if (empresa.mostrarCuit()
                && empresa.cuit() != null && !empresa.cuit().isBlank()) {
            empresaInfo.add(new Chunk("C.U.I.T.: " + empresa.cuit() + "\n", F_NORMAL));
        }
        if (empresa.mostrarCondicionIva()
                && empresa.condicionIva() != null && !empresa.condicionIva().isBlank()) {
            empresaInfo.add(new Chunk(empresa.condicionIva() + "\n", F_NORMAL));
        }
        if (empresa.iibb() != null && !empresa.iibb().isBlank()) {
            empresaInfo.add(new Chunk("IIBB: " + empresa.iibb() + "\n", F_NORMAL));
        }
        if (empresa.inicioActividad() != null && !empresa.inicioActividad().isBlank()) {
            empresaInfo.add(new Chunk("Inicio de actividad: " + empresa.inicioActividad() + "\n", F_NORMAL));
        }
        cell.addElement(empresaInfo);

        Paragraph aviso = new Paragraph("Comprobante no válido como Factura", F_SMALL_ITALIC);
        aviso.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(aviso);
        cell.addElement(Chunk.NEWLINE);

        String fecha = venta.getFechaVenta() != null
                ? venta.getFechaVenta().format(FECHA_CREDITO_FMT)
                : LocalDateTime.now().format(FECHA_CREDITO_FMT);
        Cliente cliente = venta.getCliente() != null ? venta.getCliente() : credito.getCliente();
        String nombre = TicketPDFService.nombreCliente(cliente);
        String dni = cliente != null && cliente.getDni() != null ? cliente.getDni() : "-";

        Paragraph datos = new Paragraph();
        datos.add(new Chunk("Fecha: " + fecha + "\n", F_NORMAL));
        datos.add(new Chunk("CREDITO: " + nvl(credito.getNumero(), "-") + "\n", F_BOLD));
        datos.add(new Chunk("Nombre: " + nombre + "\n", F_NORMAL));
        datos.add(new Chunk("DNI: " + dni + "\n", F_NORMAL));
        cell.addElement(datos);
        cell.addElement(Chunk.NEWLINE);

        BigDecimal total = venta.getTotal() != null ? venta.getTotal() : (credito.getImporte() != null ? credito.getImporte() : BigDecimal.ZERO);
        BigDecimal deuda = credito.getSaldo() != null ? credito.getSaldo() : (credito.getImporte() != null ? credito.getImporte() : BigDecimal.ZERO);
        BigDecimal pago = TicketPDFService.calcularPagoCredito(venta, total, deuda);

        Paragraph montos = new Paragraph();
        montos.add(new Chunk("Total a pagar: " + TicketPDFService.formatoMonedaArStatic(total) + "\n", F_BOLD));
        montos.add(new Chunk("Pago: " + TicketPDFService.formatoMonedaArStatic(pago) + "\n", F_NORMAL));
        montos.add(new Chunk("Deuda: " + TicketPDFService.formatoMonedaArStatic(deuda) + "\n", F_BOLD));
        cell.addElement(montos);
        cell.addElement(Chunk.NEWLINE);

        PdfPTable items = new PdfPTable(3);
        items.setWidthPercentage(100);
        items.setWidths(new float[]{28f, 47f, 25f});
        agregarHeader(items, "Marca");
        agregarHeader(items, "Modelo");
        agregarHeader(items, "Precio");
        if (venta.getDetalles() != null) {
            for (VentaDetalle detalle : venta.getDetalles()) {
                BigDecimal precio = detalle.getTotal() != null ? detalle.getTotal() : detalle.getSubtotal();
                agregarCeldaDato(items, TicketPDFService.marcaDetalle(detalle), Element.ALIGN_LEFT);
                agregarCeldaDato(items, TicketPDFService.modeloDetalle(detalle), Element.ALIGN_LEFT);
                agregarCeldaDato(items, TicketPDFService.formatoCreditoSinDecimales(precio), Element.ALIGN_RIGHT);
            }
        }
        cell.addElement(items);
        cell.addElement(Chunk.NEWLINE);

        if (cuotas != null && !cuotas.isEmpty()) {
            PdfPTable plan = new PdfPTable(3);
            plan.setWidthPercentage(100);
            plan.setWidths(new float[]{20f, 40f, 40f});
            agregarHeader(plan, "Cuota");
            agregarHeader(plan, "Saldo");
            agregarHeader(plan, "Vencimiento");
            for (Cuota cuota : cuotas) {
                BigDecimal saldoCuota = cuota.getSaldo() != null ? cuota.getSaldo() : cuota.getMonto();
                agregarCeldaDato(plan, TicketPDFService.numeroCuotaSimple(cuota.getNumero()), Element.ALIGN_LEFT);
                agregarCeldaDato(plan, TicketPDFService.formatoMonedaArStatic(saldoCuota), Element.ALIGN_CENTER);
                agregarCeldaDato(plan, TicketPDFService.formatearFechaCuotaCorta(cuota.getFechaVencimiento()), Element.ALIGN_CENTER);
            }
            cell.addElement(plan);
            cell.addElement(Chunk.NEWLINE);
        }

        if (incluirFirma) {
            Paragraph firma = new Paragraph();
            firma.setAlignment(Element.ALIGN_CENTER);
            firma.add(new Chunk("Firma\n", F_NORMAL));
            firma.add(new Chunk("_______________________\n\n", F_NORMAL));
            firma.add(new Chunk("Aclaración\n", F_NORMAL));
            firma.add(new Chunk("_______________________\n\n", F_NORMAL));
            firma.add(new Chunk("DNI\n", F_NORMAL));
            firma.add(new Chunk("_______________________\n", F_NORMAL));
            cell.addElement(firma);
        }

        return cell;
    }

    public byte[] generarCobroCuotas(
            List<PagoCuota> pagos,
            TicketPDFService.DatosEmpresaTicket empresa,
            TicketPDFService.DatosCobroCuotas datosCobro
    ) throws Exception {
        PagoCuota primerPago = pagos.get(0);
        Credito credito = primerPago.getCuota().getCredito();
        Cliente cliente = credito.getCliente();

        Document document = nuevoDocumentoA4();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();
        try {
            agregarCabeceraComprobanteInterno(document, empresa, "COMPROBANTE", "Cobro de cuotas",
                    primerPago.getCreatedAt() != null ? primerPago.getCreatedAt().format(FECHA_HORA_FMT) : null);
            agregarBloqueClienteSimple(document, cliente);
            agregarResumenCobroCuotas(document, datosCobro);
            agregarTablaCobroCuotas(document, pagos);
            agregarProximoVencimientoCobro(document, datosCobro);
            agregarTotalesCobro(document, pagos);
            agregarPieInterno(document);
        } finally {
            document.close();
        }
        return baos.toByteArray();
    }

    public byte[] generarResumenCuenta(ClienteCreditosResponse cuenta, TicketPDFService.DatosEmpresaTicket empresa) throws Exception {
        Document document = nuevoDocumentoA4();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();
        try {
            agregarCabeceraComprobanteInterno(document, empresa, "RESUMEN DE CUENTA",
                    "Cuenta " + (cuenta.getCuentaNumero() != null ? cuenta.getCuentaNumero() : "-"),
                    LocalDateTime.now().format(FECHA_HORA_FMT));
            agregarBloqueClienteResumen(document, cuenta);
            agregarResumenMontos(document, cuenta);
            if (cuenta.getCreditosActivos() != null) {
                for (CreditoClienteResponse credito : cuenta.getCreditosActivos()) {
                    agregarCreditoResumen(document, credito);
                }
            }
            agregarPieInterno(document);
        } finally {
            document.close();
        }
        return baos.toByteArray();
    }

    private void renderizarFacturaFiscal(
            Document document,
            FacturaAFIP factura,
            TicketPDFService.DatosEmpresaTicket empresa,
            String copia,
            Credito credito,
            List<Cuota> cuotas
    ) throws DocumentException, IOException, WriterException {
        agregarCabeceraFiscal(document, factura, empresa, copia);
        agregarBloqueClienteFiscal(document, factura);
        agregarTablaItemsFiscal(document, factura);
        if (credito != null && cuotas != null && !cuotas.isEmpty()) {
            agregarBloqueFinanciacion(document, credito, cuotas);
        }
        agregarBloqueTotalesFiscal(document, factura);
        agregarPieFiscalAutorizado(document, factura, copia);
    }

    private void renderizarComprobanteVenta(
            Document document,
            Venta venta,
            TicketPDFService.DatosEmpresaTicket empresa,
            Credito credito,
            List<Cuota> cuotas
    ) throws DocumentException {
        String subtitulo = credito != null ? "Crédito personal" : descripcionTipoVenta(venta);
        String fecha = venta.getFechaVenta() != null ? venta.getFechaVenta().format(FECHA_HORA_FMT) : null;
        agregarCabeceraComprobanteInterno(document, empresa, "COMPROBANTE", subtitulo, fecha);
        agregarBloqueClienteVenta(document, venta);
        agregarTablaItemsVenta(document, venta);
        agregarTotalesVenta(document, venta);
        agregarPagosVenta(document, venta.getPagos(), credito);
        if (credito != null) {
            agregarResumenCredito(document, credito);
        }
        if (cuotas != null && !cuotas.isEmpty()) {
            agregarPlanCuotas(document, cuotas);
        }
        agregarPieInterno(document);
    }

    private void agregarCabeceraFiscal(
            Document document,
            FacturaAFIP factura,
            TicketPDFService.DatosEmpresaTicket empresa,
            String copia
    ) throws DocumentException {
        String ptoVta = String.format("%05d", factura.getPtoVta());
        String nro = String.format("%08d", factura.getCbteNro());
        String cuit = cuitSinFormato(empresa.cuit());
        String iibb = nvl(empresa.iibb(), cuit);
        String inicio = nvl(empresa.inicioActividad(), "-");

        PdfPTable contenedor = new PdfPTable(1);
        contenedor.setWidthPercentage(100);

        PdfPTable header = new PdfPTable(3);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{46f, 12f, 42f});

        Paragraph izq = new Paragraph();
        izq.add(new Chunk(empresa.razonSocial() + "\n", F_EMPRESA));
        izq.add(lineaCampo("Razón Social:", empresa.razonSocial()));
        izq.add(lineaCampo("Domicilio Comercial:", nvl(empresa.direccion())));
        izq.add(lineaCampo("Condición frente al IVA:", nvl(empresa.condicionIva(), "IVA Responsable Inscripto")));
        PdfPCell celdaIzq = celdaContenido(izq);
        celdaIzq.setBorder(Rectangle.LEFT | Rectangle.TOP | Rectangle.BOTTOM);
        celdaIzq.setPadding(8);
        header.addCell(celdaIzq);

        PdfPCell celdaCentro = new PdfPCell();
        celdaCentro.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        celdaCentro.setPadding(4);
        celdaCentro.setHorizontalAlignment(Element.ALIGN_CENTER);
        celdaCentro.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph copiaP = new Paragraph(copia, F_BOLD);
        copiaP.setAlignment(Element.ALIGN_CENTER);
        celdaCentro.addElement(copiaP);
        celdaCentro.addElement(crearRecuadroLetra(factura));
        header.addCell(celdaCentro);

        Paragraph der = new Paragraph();
        der.setAlignment(Element.ALIGN_LEFT);
        der.add(new Chunk("FACTURA\n", F_FACTURA));
        der.add(lineaCampo("Punto de Venta:", ptoVta));
        der.add(lineaCampo("Comp. Nro:", nro));
        der.add(lineaCampo("Fecha de Emisión:", formatearFechaAfip(factura.getCbteFch())));
        der.add(lineaCampo("CUIT:", cuit));
        der.add(lineaCampo("Ingresos Brutos:", iibb));
        der.add(lineaCampo("Fecha de Inicio de Actividades:", inicio));
        PdfPCell celdaDer = celdaContenido(der);
        celdaDer.setBorder(Rectangle.RIGHT | Rectangle.TOP | Rectangle.BOTTOM);
        celdaDer.setPadding(8);
        header.addCell(celdaDer);

        PdfPCell wrapper = new PdfPCell(header);
        wrapper.setBorder(Rectangle.BOX);
        wrapper.setPadding(0);
        contenedor.addCell(wrapper);
        document.add(contenedor);
    }

    private PdfPTable crearRecuadroLetra(FacturaAFIP factura) {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(70);
        box.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell inner = new PdfPCell();
        inner.setBorder(Rectangle.BOX);
        inner.setPadding(6);
        inner.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk(letraComprobante(factura.getCbteTipo()) + "\n", F_LETRA));
        p.add(new Chunk("COD. " + String.format("%03d", factura.getCbteTipo()), F_BOLD));
        inner.addElement(p);
        box.addCell(inner);
        return box;
    }

    private Paragraph lineaCampo(String etiqueta, String valor) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(etiqueta + " ", F_NORMAL));
        p.add(new Chunk(nvl(valor, "-"), F_NORMAL));
        return p;
    }

    private void agregarBloqueClienteFiscal(Document document, FacturaAFIP factura) throws DocumentException {
        ClienteAFIP cliente = factura.getClienteAFIP();
        Venta venta = factura.getVenta();
        PdfPTable bloque = new PdfPTable(1);
        bloque.setWidthPercentage(100);

        String nombre = cliente != null && cliente.getRazonSocial() != null && !cliente.getRazonSocial().isBlank()
                ? cliente.getRazonSocial()
                : "A CONSUMIDOR FINAL";
        String domicilio = cliente != null && cliente.getDomicilio() != null ? cliente.getDomicilio() : "";
        String condIva = cliente != null
                ? etiquetaCondicionIvaReceptor(cliente.getCondicionIVAReceptorId())
                : "Consumidor Final";
        String condVenta = condicionVenta(venta);
        String docValor = "-";
        if (cliente != null && cliente.getDocNro() != null && !cliente.getDocNro().isBlank() && !"0".equals(cliente.getDocNro())) {
            docValor = cliente.getDocTipo() != null && cliente.getDocTipo() == 80
                    ? cuitSinFormato(cliente.getDocNro())
                    : cliente.getDocNro();
        }

        Paragraph p = new Paragraph();
        p.add(lineaCampo("Doc.:", docValor));
        p.add(lineaCampo("Apellido y Nombre / Razón Social:", nombre));
        p.add(lineaCampo("Condición frente al IVA:", condIva));
        p.add(lineaCampo("Domicilio:", domicilio.isBlank() ? "" : domicilio));
        p.add(lineaCampo("Condición de venta:", condVenta));
        bloque.addCell(celdaContenido(p));
        document.add(bloque);
        document.add(Chunk.NEWLINE);
    }

    private void agregarTablaItemsFiscal(Document document, FacturaAFIP factura) throws DocumentException {
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{10f, 28f, 9f, 10f, 12f, 8f, 10f, 13f});

        agregarHeader(table, "Código");
        agregarHeader(table, "Producto / Servicio");
        agregarHeader(table, "Cantidad");
        agregarHeader(table, "U. Medida");
        agregarHeader(table, "Precio Unit.");
        agregarHeader(table, "% Bonif");
        agregarHeader(table, "Imp. Bonif.");
        agregarHeader(table, "Subtotal");

        int cantidadItems = factura.getItems() != null ? factura.getItems().size() : 0;
        if (factura.getItems() != null) {
            for (FacturaItemAFIP item : factura.getItems()) {
                agregarCeldaDato(table, nvl(item.getCodigo()), Element.ALIGN_LEFT);
                agregarCeldaDato(table, nvl(item.getDescripcion()), Element.ALIGN_LEFT);
                agregarCeldaDato(table, formatoCantidad(item.getCantidad()), Element.ALIGN_RIGHT);
                agregarCeldaDato(table, "unidades", Element.ALIGN_CENTER);
                agregarCeldaDato(table, formatoImporte(item.getPrecioUnitario()), Element.ALIGN_RIGHT);
                agregarCeldaDato(table, "0,00", Element.ALIGN_RIGHT);
                agregarCeldaDato(table, "0,00", Element.ALIGN_RIGHT);
                agregarCeldaDato(table, formatoImporte(item.getSubtotal()), Element.ALIGN_RIGHT);
            }
        }

        int filasVacias = Math.max(0, 10 - cantidadItems);
        for (int i = 0; i < filasVacias; i++) {
            for (int c = 0; c < 8; c++) {
                agregarCeldaDato(table, " ", Element.ALIGN_LEFT);
            }
        }

        document.add(table);
    }

    private void agregarBloqueTotalesFiscal(Document document, FacturaAFIP factura) throws DocumentException {
        BigDecimal subtotal = factura.getImpNeto() != null ? factura.getImpNeto() : factura.getImpTotal();
        BigDecimal tributos = factura.getImpTrib() != null ? factura.getImpTrib() : BigDecimal.ZERO;
        BigDecimal total = factura.getImpTotal() != null ? factura.getImpTotal() : BigDecimal.ZERO;
        BigDecimal iva = factura.getImpIVA() != null ? factura.getImpIVA() : BigDecimal.ZERO;

        PdfPTable contenedor = new PdfPTable(1);
        contenedor.setWidthPercentage(100);

        PdfPTable bloque = new PdfPTable(1);
        bloque.setWidthPercentage(100);

        PdfPTable totales = new PdfPTable(2);
        totales.setWidthPercentage(45);
        totales.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totales.setWidths(new float[]{62f, 38f});
        agregarFilaTotalFiscal(totales, "Subtotal: $", formatoImporte(subtotal));
        agregarFilaTotalFiscal(totales, "Importe Otros Tributos: $", formatoImporte(tributos));
        agregarFilaTotalFiscal(totales, "Importe Total: $", formatoImporte(total), true);

        PdfPCell celdaTotales = new PdfPCell();
        celdaTotales.setBorder(Rectangle.NO_BORDER);
        celdaTotales.setPadding(8);
        celdaTotales.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaTotales.addElement(totales);
        bloque.addCell(celdaTotales);

        PdfPCell separador = new PdfPCell();
        separador.setBorder(Rectangle.TOP);
        separador.setFixedHeight(1f);
        bloque.addCell(separador);

        Paragraph transparencia = new Paragraph();
        transparencia.setAlignment(Element.ALIGN_CENTER);
        Font leyFont = new Font(F_SMALL.getFamily(), F_SMALL.getSize(), Font.UNDERLINE);
        transparencia.add(new Chunk("Régimen de Transparencia Fiscal al Consumidor (Ley 27.743)\n", leyFont));
        transparencia.add(new Chunk("IVA Contenido: $ " + formatoImporte(iva), F_BOLD));
        PdfPCell celdaTransp = new PdfPCell(transparencia);
        celdaTransp.setBorder(Rectangle.NO_BORDER);
        celdaTransp.setPadding(8);
        celdaTransp.setHorizontalAlignment(Element.ALIGN_CENTER);
        bloque.addCell(celdaTransp);

        PdfPCell wrapper = new PdfPCell(bloque);
        wrapper.setBorder(Rectangle.BOX);
        wrapper.setPadding(0);
        contenedor.addCell(wrapper);
        document.add(contenedor);
        document.add(Chunk.NEWLINE);
    }

    private void agregarPieFiscalAutorizado(Document document, FacturaAFIP factura, String copia)
            throws DocumentException, IOException, WriterException {
        PdfPTable pie = new PdfPTable(4);
        pie.setWidthPercentage(100);
        pie.setWidths(new float[]{18f, 34f, 18f, 30f});

        PdfPCell qrCell = new PdfPCell();
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setPadding(4);
        qrCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Image qr = generarImagenQr(factura);
        qr.scaleToFit(88, 88);
        qrCell.addElement(qr);
        pie.addCell(qrCell);

        PdfPCell arcaCell = new PdfPCell();
        arcaCell.setBorder(Rectangle.NO_BORDER);
        arcaCell.setPadding(4);
        arcaCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph arca = new Paragraph();
        arca.setAlignment(Element.ALIGN_CENTER);
        arca.add(new Chunk("ARCA\n", F_ARCA));
        arca.add(new Chunk("Agencia de Recaudación\n", F_SMALL));
        arca.add(new Chunk("y Control Aduanero\n", F_SMALL));
        arca.add(new Chunk("\nComprobante Autorizado\n", F_BOLD));
        arca.add(new Chunk("Esta Agencia no se responsabiliza por los datos ingresados en el detalle de la operación", F_SMALL_ITALIC));
        arcaCell.addElement(arca);
        pie.addCell(arcaCell);

        PdfPCell pagCell = new PdfPCell(new Phrase("Pág. 1/1", F_NORMAL));
        pagCell.setBorder(Rectangle.NO_BORDER);
        pagCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        pagCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        pie.addCell(pagCell);

        Paragraph cae = new Paragraph();
        cae.setAlignment(Element.ALIGN_RIGHT);
        if (factura.getCae() != null && !factura.getCae().isBlank()) {
            cae.add(lineaCampo("CAE N°:", factura.getCae()));
        }
        if (factura.getCaeFchVto() != null && !factura.getCaeFchVto().isBlank()) {
            cae.add(lineaCampo("Fecha de Vto. de CAE:", formatearFechaAfip(factura.getCaeFchVto())));
        }
        PdfPCell caeCell = new PdfPCell(cae);
        caeCell.setBorder(Rectangle.NO_BORDER);
        caeCell.setPadding(4);
        caeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        caeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        pie.addCell(caeCell);

        document.add(pie);
    }

    private void agregarCabeceraComprobanteInterno(
            Document document,
            TicketPDFService.DatosEmpresaTicket empresa,
            String titulo,
            String subtitulo,
            String fecha
    ) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{60f, 40f});

        Paragraph izq = new Paragraph();
        izq.add(new Chunk(empresa.razonSocial() + "\n", F_BOLD));
        if (empresa.mostrarDireccion()) {
            izq.add(new Chunk("Domicilio: " + nvl(empresa.direccion()) + "\n", F_NORMAL));
        }
        if (empresa.mostrarCuit() && empresa.cuit() != null && !empresa.cuit().isBlank()) {
            izq.add(new Chunk("CUIT: " + formatearCuit(empresa.cuit()) + "\n", F_NORMAL));
        }
        if (empresa.mostrarCondicionIva()
                && empresa.condicionIva() != null && !empresa.condicionIva().isBlank()) {
            izq.add(new Chunk(empresa.condicionIva() + "\n", F_NORMAL));
        }
        header.addCell(celdaContenido(izq));

        Paragraph der = new Paragraph();
        der.setAlignment(Element.ALIGN_RIGHT);
        der.add(new Chunk(titulo + "\n", F_FACTURA));
        if (subtitulo != null && !subtitulo.isBlank()) {
            der.add(new Chunk(subtitulo + "\n", F_NORMAL));
        }
        if (fecha != null) {
            der.add(new Chunk("Fecha: " + fecha + "\n", F_NORMAL));
        }
        header.addCell(celdaContenido(der));
        document.add(header);
        document.add(Chunk.NEWLINE);
    }

    private void agregarBloqueClienteSimple(Document document, Cliente cliente) throws DocumentException {
        PdfPTable bloque = new PdfPTable(1);
        bloque.setWidthPercentage(100);
        Paragraph p = new Paragraph();
        if (cliente != null) {
            String nombre = ((nvl(cliente.getNombre()) + " " + nvl(cliente.getApellido())).trim());
            if (!nombre.isBlank()) {
                p.add(new Chunk("Cliente: " + nombre + "\n", F_NORMAL));
            }
            if (cliente.getDni() != null && !cliente.getDni().isBlank()) {
                p.add(new Chunk("DNI: " + cliente.getDni() + "\n", F_NORMAL));
            }
        } else {
            p.add(new Chunk("Cliente: Consumidor final\n", F_NORMAL));
        }
        bloque.addCell(celdaContenido(p));
        document.add(bloque);
        document.add(Chunk.NEWLINE);
    }

    private void agregarBloqueClienteVenta(Document document, Venta venta) throws DocumentException {
        PdfPTable bloque = new PdfPTable(1);
        bloque.setWidthPercentage(100);
        Paragraph p = new Paragraph();
        Cliente cliente = venta.getCliente();
        if (cliente != null) {
            String nombre = ((nvl(cliente.getNombre()) + " " + nvl(cliente.getApellido())).trim());
            if (!nombre.isBlank()) {
                p.add(new Chunk("Cliente: " + nombre + "\n", F_NORMAL));
            }
            if (cliente.getDni() != null && !cliente.getDni().isBlank()) {
                p.add(new Chunk("DNI: " + cliente.getDni() + "\n", F_NORMAL));
            }
        } else {
            p.add(new Chunk("Cliente: Consumidor final\n", F_NORMAL));
        }
        if (venta.getNumeroFactura() != null) {
            p.add(new Chunk("N° comprobante interno: " + venta.getNumeroFactura() + "\n", F_NORMAL));
        }
        bloque.addCell(celdaContenido(p));
        document.add(bloque);
        document.add(Chunk.NEWLINE);
    }

    private void agregarBloqueClienteResumen(Document document, ClienteCreditosResponse cuenta) throws DocumentException {
        PdfPTable bloque = new PdfPTable(1);
        bloque.setWidthPercentage(100);
        Paragraph p = new Paragraph();
        String nombre = ((nvl(cuenta.getClienteNombre()) + " " + nvl(cuenta.getClienteApellido())).trim());
        if (!nombre.isBlank()) {
            p.add(new Chunk("Cliente: " + nombre + "\n", F_NORMAL));
        }
        if (cuenta.getClienteDni() != null && !cuenta.getClienteDni().isBlank()) {
            p.add(new Chunk("DNI: " + cuenta.getClienteDni() + "\n", F_NORMAL));
        }
        bloque.addCell(celdaContenido(p));
        document.add(bloque);
        document.add(Chunk.NEWLINE);
    }

    private void agregarTablaItemsVenta(Document document, Venta venta) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{8f, 42f, 10f, 15f, 25f});
        agregarHeader(table, "Cant.");
        agregarHeader(table, "Descripción");
        agregarHeader(table, "IVA");
        agregarHeader(table, "P. Unit.");
        agregarHeader(table, "Subtotal");

        if (venta.getDetalles() != null) {
            for (VentaDetalle detalle : venta.getDetalles()) {
                agregarCeldaDato(table, String.valueOf(detalle.getCantidad()), Element.ALIGN_CENTER);
                agregarCeldaDato(table, descripcionDetalle(detalle), Element.ALIGN_LEFT);
                agregarCeldaDato(table, obtenerIvaDetalle(detalle), Element.ALIGN_CENTER);
                agregarCeldaDato(table, formatoMoneda(detalle.getPrecioUnitario()), Element.ALIGN_RIGHT);
                BigDecimal total = detalle.getTotal() != null ? detalle.getTotal() : detalle.getSubtotal();
                agregarCeldaDato(table, formatoMoneda(total), Element.ALIGN_RIGHT);
            }
        }
        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void agregarTotalesVenta(Document document, Venta venta) throws DocumentException {
        PdfPTable outer = new PdfPTable(2);
        outer.setWidthPercentage(100);
        outer.addCell(celdaVacia());
        PdfPCell cell = celdaSinBorde();
        PdfPTable totales = new PdfPTable(2);
        totales.setWidthPercentage(100);
        agregarFilaTotal(totales, "Importe Total:", formatoMoneda(venta.getTotal()), true);
        cell.addElement(totales);
        outer.addCell(cell);
        document.add(outer);
        document.add(Chunk.NEWLINE);
    }

    private void agregarPagosVenta(Document document, Collection<PagoVenta> pagos, Credito credito) throws DocumentException {
        if (pagos == null || pagos.isEmpty()) {
            return;
        }
        List<PagoVenta> visibles = pagos.stream()
                .filter(p -> p.getMonto() != null && p.getMonto().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        if (visibles.isEmpty() || (visibles.size() <= 1 && credito == null)) {
            return;
        }

        Paragraph titulo = new Paragraph("Formas de pago", F_BOLD);
        document.add(titulo);
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        for (PagoVenta pago : visibles) {
            agregarCeldaDato(table, etiquetaMetodoPago(pago.getMetodoPago()), Element.ALIGN_LEFT);
            agregarCeldaDato(table, formatoMoneda(pago.getMonto()), Element.ALIGN_RIGHT);
        }
        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void agregarResumenCredito(Document document, Credito credito) throws DocumentException {
        PdfPTable bloque = new PdfPTable(1);
        bloque.setWidthPercentage(100);
        Paragraph p = new Paragraph();
        p.add(new Chunk("Financiación\n", F_BOLD));
        p.add(new Chunk("Importe financiado: " + formatoMoneda(credito.getImporte()) + "\n", F_NORMAL));
        if (credito.getSaldo() != null) {
            p.add(new Chunk("Saldo pendiente: " + formatoMoneda(credito.getSaldo()) + "\n", F_NORMAL));
        }
        if (credito.getPlazoMeses() != null) {
            p.add(new Chunk("Plazo: " + credito.getPlazoMeses() + " cuotas\n", F_NORMAL));
        }
        bloque.addCell(celdaContenido(p));
        document.add(bloque);
        document.add(Chunk.NEWLINE);
    }

    private void agregarBloqueFinanciacion(Document document, Credito credito, List<Cuota> cuotas) throws DocumentException {
        agregarResumenCredito(document, credito);
        agregarPlanCuotas(document, cuotas);
    }

    private void agregarPlanCuotas(Document document, List<Cuota> cuotas) throws DocumentException {
        Paragraph titulo = new Paragraph("Detalle de cuotas", F_BOLD);
        document.add(titulo);
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{15f, 25f, 30f, 30f});
        agregarHeader(table, "N°");
        agregarHeader(table, "Vencimiento");
        agregarHeader(table, "Monto");
        agregarHeader(table, "Estado");
        for (Cuota cuota : cuotas) {
            agregarCeldaDato(table, nvl(cuota.getNumero(), "-"), Element.ALIGN_LEFT);
            agregarCeldaDato(table, formatearFechaCuota(cuota.getFechaVencimiento()), Element.ALIGN_LEFT);
            agregarCeldaDato(table, formatoMoneda(cuota.getMonto()), Element.ALIGN_RIGHT);
            agregarCeldaDato(table, etiquetaEstadoCuota(cuota.getEstado()), Element.ALIGN_CENTER);
        }
        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void agregarResumenCobroCuotas(
            Document document,
            TicketPDFService.DatosCobroCuotas datos
    ) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        agregarFilaTotal(table, "Total del crédito:", formatoMoneda(datos.totalCredito()));
        agregarFilaTotal(table, "Saldo del crédito:", formatoMoneda(datos.saldoCredito()));
        agregarFilaTotal(table, "Saldo de todos los créditos:", formatoMoneda(datos.saldoTodosCreditos()), true);
        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void agregarTablaCobroCuotas(
            Document document,
            List<PagoCuota> pagos
    ) throws DocumentException {
        Paragraph titulo = new Paragraph("DETALLE DEL COBRO", F_BOLD);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50f, 50f});
        for (PagoCuota pago : pagos) {
            Cuota cuota = pago.getCuota();
            BigDecimal saldo = cuota.getSaldo() != null ? cuota.getSaldo() : BigDecimal.ZERO;
            BigDecimal recargo = cuota.getRecargo() != null ? cuota.getRecargo() : BigDecimal.ZERO;
            // Fila 1: cuota + total
            agregarCeldaSinBorde(table, "Cuota " + TicketPDFService.numeroCuotaFraccion(cuota.getNumero()), Element.ALIGN_LEFT);
            agregarCeldaSinBorde(table, "Total " + formatoMoneda(cuota.getMonto()), Element.ALIGN_RIGHT);
            // Fila 2: recargo + saldo
            agregarCeldaSinBorde(table, "Recargo " + formatoMoneda(recargo), Element.ALIGN_LEFT);
            agregarCeldaSinBorde(table, "Saldo " + formatoMoneda(saldo.add(recargo)), Element.ALIGN_RIGHT);
        }
        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void agregarProximoVencimientoCobro(
            Document document,
            TicketPDFService.DatosCobroCuotas datos
    ) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50f, 50f});
        agregarCeldaSinBorde(table, "Próx. cuota " + nvl(datos.proximaCuotaNumero(), "-")
                + " · " + nvl(datos.proximoVencimiento(), "-"), Element.ALIGN_LEFT);
        agregarCeldaSinBorde(table, "Saldo " + formatoMoneda(datos.saldoProximoVencimiento()), Element.ALIGN_RIGHT);
        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void agregarTotalesCobro(Document document, List<PagoCuota> pagos) throws DocumentException {
        BigDecimal total = pagos.stream().map(PagoCuota::getMonto).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        String metodo = pagos.get(0).getMetodoPago() != null ? etiquetaMetodoPago(pagos.get(0).getMetodoPago()) : "Pago";
        PdfPTable outer = new PdfPTable(2);
        outer.setWidthPercentage(100);
        outer.addCell(celdaVacia());
        PdfPCell cell = celdaSinBorde();
        PdfPTable totales = new PdfPTable(2);
        totales.setWidthPercentage(100);
        agregarFilaTotal(totales, "Método de pago:", metodo);
        agregarFilaTotal(totales, "Total cobrado:", formatoMoneda(total), true);
        cell.addElement(totales);
        outer.addCell(cell);
        document.add(outer);
        document.add(Chunk.NEWLINE);
    }

    private void agregarResumenMontos(Document document, ClienteCreditosResponse cuenta) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        agregarFilaTotal(table, "Saldo pendiente:", formatoMoneda(cuenta.getSaldoCuenta()));
        agregarFilaTotal(table, "Créditos activos:",
                String.valueOf(cuenta.getCantidadCreditos() != null ? cuenta.getCantidadCreditos() : 0));
        agregarFilaTotal(table, "Total créditos:", formatoMoneda(cuenta.getTotalCreditosSacados()));
        agregarFilaTotal(table, "Total pagado:", formatoMoneda(cuenta.getTotalPagado()));
        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void agregarCreditoResumen(Document document, CreditoClienteResponse credito) throws DocumentException {
        Paragraph titulo = new Paragraph("Crédito " + nvl(credito.getNumero(), "-"), F_BOLD);
        document.add(titulo);
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        agregarFilaTotal(info, "Saldo:", formatoMoneda(credito.getSaldo()));
        agregarFilaTotal(info, "Estado:", nvl(credito.getEstado(), "-"));
        document.add(info);

        if (credito.getCuotas() != null && !credito.getCuotas().isEmpty()) {
            PdfPTable cuotas = new PdfPTable(4);
            cuotas.setWidthPercentage(100);
            agregarHeader(cuotas, "Cuota");
            agregarHeader(cuotas, "Venc.");
            agregarHeader(cuotas, "Saldo");
            agregarHeader(cuotas, "Estado");
            for (CuotaCreditoResponse cuota : credito.getCuotas()) {
                agregarCeldaDato(cuotas, nvl(cuota.getNumero(), "-"), Element.ALIGN_LEFT);
                agregarCeldaDato(cuotas, formatearFechaCuota(cuota.getFechaVencimiento()), Element.ALIGN_LEFT);
                agregarCeldaDato(cuotas, formatoMoneda(cuota.getSaldo()), Element.ALIGN_RIGHT);
                agregarCeldaDato(cuotas, nvl(cuota.getEstado(), "-"), Element.ALIGN_CENTER);
            }
            document.add(cuotas);
        }
        document.add(Chunk.NEWLINE);
    }

    private void agregarPieInterno(Document document) throws DocumentException {
        Paragraph p = new Paragraph("Gracias por su compra", F_SMALL);
        p.setAlignment(Element.ALIGN_CENTER);
        document.add(p);
    }

    private Image generarImagenQr(FacturaAFIP factura) throws IOException, WriterException, BadElementException {
        String qrData = generarDatosQr(factura);
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 200, 200);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "PNG", baos);
        return Image.getInstance(baos.toByteArray());
    }

    private String generarDatosQr(FacturaAFIP factura) {
        try {
            StringBuilder json = new StringBuilder("{");
            json.append("\"ver\":1,");
            json.append("\"fecha\":\"").append(formatearFechaJson(factura.getCbteFch())).append("\",");
            json.append("\"cuit\":").append(AfipContextHolder.require().cuitSinGuiones()).append(",");
            json.append("\"ptoVta\":").append(factura.getPtoVta()).append(",");
            json.append("\"tipoCmp\":").append(factura.getCbteTipo()).append(",");
            json.append("\"nroCmp\":").append(factura.getCbteNro()).append(",");
            json.append("\"importe\":").append(factura.getImpTotal().toPlainString()).append(",");
            json.append("\"moneda\":\"").append(factura.getMonId() != null ? factura.getMonId() : "PES").append("\",");
            BigDecimal cotiz = factura.getMonCotiz() != null ? factura.getMonCotiz() : BigDecimal.ONE;
            json.append("\"ctz\":").append(cotiz.toPlainString()).append(",");
            if (factura.getClienteAFIP() != null) {
                Integer docTipo = factura.getClienteAFIP().getDocTipo();
                String docNro = factura.getClienteAFIP().getDocNro();
                if (docTipo != null && docTipo != 99 && docNro != null && !docNro.isBlank() && !"0".equals(docNro)) {
                    json.append("\"tipoDocRec\":").append(docTipo).append(",");
                    json.append("\"nroDocRec\":").append(docNro).append(",");
                }
            }
            json.append("\"tipoCodAut\":\"E\",");
            json.append("\"codAut\":").append(factura.getCae().trim());
            json.append("}");
            return "https://www.arca.gob.ar/fe/qr/?p="
                    + java.util.Base64.getEncoder().encodeToString(json.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            log.warn("Error generando QR fiscal A4: {}", e.getMessage());
            return "https://www.arca.gob.ar/fe/qr/";
        }
    }

    private Document nuevoDocumentoA4() {
        Document document = new Document(PageSize.A4);
        document.setMargins(MARGEN, MARGEN, MARGEN, MARGEN);
        return document;
    }

    private static DecimalFormat crearFormatoMoneda() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "AR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0.00", symbols);
    }

    private String formatoMoneda(BigDecimal valor) {
        if (valor == null) {
            return "$ 0,00";
        }
        return "$ " + MONEDA_FMT.format(valor);
    }

    private String formatoImporte(BigDecimal valor) {
        if (valor == null) {
            return "0,00";
        }
        return MONEDA_FMT.format(valor);
    }

    private String cuitSinFormato(String cuit) {
        if (cuit == null) {
            return "";
        }
        return cuit.replaceAll("\\D", "");
    }

    private void agregarFilaTotalFiscal(PdfPTable table, String etiqueta, String valor) {
        agregarFilaTotalFiscal(table, etiqueta, valor, false);
    }

    private void agregarFilaTotalFiscal(PdfPTable table, String etiqueta, String valor, boolean bold) {
        Font font = bold ? F_BOLD : F_NORMAL;
        PdfPCell l = new PdfPCell(new Phrase(etiqueta, font));
        l.setBorder(Rectangle.NO_BORDER);
        l.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(3);
        PdfPCell r = new PdfPCell(new Phrase(valor, font));
        r.setBorder(Rectangle.NO_BORDER);
        r.setHorizontalAlignment(Element.ALIGN_RIGHT);
        r.setPadding(3);
        table.addCell(l);
        table.addCell(r);
    }

    private String formatoCantidad(BigDecimal valor) {
        if (valor == null) {
            return "0,00";
        }
        return MONEDA_FMT.format(valor);
    }

    private String formatearFechaAfip(String fechaYyyymmdd) {
        if (fechaYyyymmdd == null || fechaYyyymmdd.length() != 8) {
            return nvl(fechaYyyymmdd, "-");
        }
        return fechaYyyymmdd.substring(6, 8) + "/" + fechaYyyymmdd.substring(4, 6) + "/" + fechaYyyymmdd.substring(0, 4);
    }

    private String formatearFechaJson(String fechaYyyymmdd) {
        if (fechaYyyymmdd == null || fechaYyyymmdd.length() != 8) {
            return LocalDateTime.now().toLocalDate().toString();
        }
        return fechaYyyymmdd.substring(0, 4) + "-" + fechaYyyymmdd.substring(4, 6) + "-" + fechaYyyymmdd.substring(6, 8);
    }

    private String formatearFechaCuota(LocalDateTime fecha) {
        if (fecha == null) {
            return "-";
        }
        return String.format("%02d/%02d/%04d", fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getYear());
    }

    private String formatearCuit(String cuit) {
        if (cuit == null) {
            return "";
        }
        String digits = cuit.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return digits.substring(0, 2) + "-" + digits.substring(2, 10) + "-" + digits.substring(10);
        }
        return cuit;
    }

    private String letraComprobante(Integer cbteTipo) {
        if (cbteTipo == null) {
            return "?";
        }
        return switch (cbteTipo) {
            case 1 -> "A";
            case 6 -> "B";
            case 11 -> "C";
            default -> "?";
        };
    }

    private String etiquetaCondicionIvaReceptor(Integer id) {
        if (id == null) {
            return "Consumidor Final";
        }
        return switch (id) {
            case 1 -> "IVA Responsable Inscripto";
            case 4 -> "IVA Sujeto Exento";
            case 5 -> "Consumidor Final";
            case 6 -> "Responsable Monotributo";
            case 7 -> "Sujeto No Categorizado";
            case 13 -> "Monotributista Social";
            case 15 -> "IVA No Alcanzado";
            case 16 -> "Monotributo Trabajador Independiente Promovido";
            default -> "Condición " + id;
        };
    }

    private String condicionVenta(Venta venta) {
        if (venta == null) {
            return "Contado";
        }
        String metodo = venta.getMetodoPago();
        if (metodo == null || metodo.isBlank()) {
            return venta.getPagos() != null && venta.getPagos().size() > 1 ? "Otra" : "Contado";
        }
        return switch (metodo.trim().toUpperCase()) {
            case "EFECTIVO" -> "Contado";
            case "TARJETA DE CREDITO", "TARJETA CREDITO" -> "Tarjeta de Crédito";
            case "TARJETA DE DEBITO", "TARJETA DEBITO" -> "Tarjeta de Débito";
            case "TRANSFERENCIA", "QR" -> "Transferencia bancaria";
            case "CREDITO" -> "Cuenta Corriente";
            default -> metodo;
        };
    }

    private String etiquetaDocumentoCliente(ClienteAFIP cliente) {
        if (cliente == null) {
            return "Doc.: -";
        }
        String etiqueta = switch (cliente.getDocTipo() != null ? cliente.getDocTipo() : 99) {
            case 80 -> "CUIT";
            case 96 -> "DNI";
            case 86 -> "CUIL";
            default -> "Doc.";
        };
        String nro = cliente.getDocNro();
        if (cliente.getDocTipo() != null && cliente.getDocTipo() == 80) {
            nro = formatearCuit(nro);
        }
        return etiqueta + ": " + nvl(nro, "-");
    }

    private String descripcionTipoVenta(Venta venta) {
        if (venta.getPagos() != null && venta.getPagos().size() > 1) {
            return "Pagos múltiples";
        }
        return etiquetaMetodoPago(venta.getMetodoPago());
    }

    private String etiquetaMetodoPago(String metodo) {
        if (metodo == null || metodo.isBlank()) {
            return "Venta";
        }
        return switch (metodo.trim().toUpperCase()) {
            case "EFECTIVO" -> "Efectivo";
            case "TRANSFERENCIA" -> "Transferencia";
            case "CREDITO" -> "Crédito";
            case "COMBINADO" -> "Pagos múltiples";
            case "QR" -> "QR";
            case "TARJETA DE CREDITO", "TARJETA CREDITO" -> "Tarjeta de crédito";
            case "TARJETA DE DEBITO", "TARJETA DEBITO" -> "Tarjeta de débito";
            default -> metodo;
        };
    }

    private String descripcionDetalle(VentaDetalle detalle) {
        return VentaDetalleSupport.descripcionLinea(detalle);
    }

    private String obtenerIvaDetalle(VentaDetalle detalle) {
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

    private String etiquetaEstadoCuota(Cuota.EstadoCuota estado) {
        if (estado == null) {
            return "-";
        }
        return switch (estado) {
            case PENDIENTE -> "Pendiente";
            case PAGADA -> "Pagada";
            case VENCIDA -> "Vencida";
            case CANCELADA -> "Cancelada";
            case ELIMINADA -> "Eliminada";
        };
    }

    private void agregarHeader(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, F_BOLD));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(GRIS_HEADER);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void agregarCeldaDato(PdfPTable table, String texto, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(nvl(texto), F_NORMAL));
        cell.setHorizontalAlignment(align);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void agregarHeaderSinBorde(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, F_BOLD));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void agregarCeldaSinBorde(PdfPTable table, String texto, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(nvl(texto), F_NORMAL));
        cell.setHorizontalAlignment(align);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void agregarFilaTotal(PdfPTable table, String etiqueta, String valor) {
        agregarFilaTotal(table, etiqueta, valor, false);
    }

    private void agregarFilaTotal(PdfPTable table, String etiqueta, String valor, boolean bold) {
        Font font = bold ? F_BOLD : F_NORMAL;
        PdfPCell l = new PdfPCell(new Phrase(etiqueta, font));
        l.setBorder(Rectangle.NO_BORDER);
        l.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(3);
        PdfPCell r = new PdfPCell(new Phrase(valor, font));
        r.setBorder(Rectangle.NO_BORDER);
        r.setHorizontalAlignment(Element.ALIGN_RIGHT);
        r.setPadding(3);
        table.addCell(l);
        table.addCell(r);
    }

    private PdfPCell celda(String texto, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell celdaContenido(Paragraph paragraph) {
        PdfPCell cell = new PdfPCell();
        cell.addElement(paragraph);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell celdaSinBorde() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(2);
        return cell;
    }

    private PdfPCell celdaVacia() {
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }

    private String nvl(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
