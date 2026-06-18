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
import com.vida.apirest.config.AfipProperties;
import com.vida.apirest.model.afip.FacturaAFIP;
import com.vida.apirest.model.afip.FacturaItemAFIP;
import com.vida.apirest.model.afip.FacturaIvaAFIP;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "afip.enabled", havingValue = "true")
public class TicketPDFService {

    private final AfipProperties afipProperties;
    
    private static final Font FONT_NORMAL = new Font(Font.FontFamily.COURIER, 10, Font.NORMAL);
    private static final Font FONT_BOLD = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);
    private static final Font FONT_LARGE = new Font(Font.FontFamily.COURIER, 16, Font.BOLD);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    
    /**
     * Genera un PDF del ticket de factura AFIP
     * @param facturaAFIP La factura AFIP a imprimir
     * @param outputPath Ruta donde guardar el PDF
     * @throws Exception Si hay error al generar el PDF
     */
    public void generarTicketPDF(FacturaAFIP facturaAFIP, String outputPath) throws Exception {
        // Tamaño de ticket: 8cm de ancho (aproximadamente 226 puntos)
        float anchoTicket = 226f; // 8cm
        float altoTicket = 800f; // Alto suficiente para el contenido
        Document document = new Document(new Rectangle(anchoTicket, altoTicket));
        document.setMargins(15, 15, 15, 15);
        
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));
        document.open();
        
        try {
            // Información de la empresa
            agregarInfoEmpresa(document);
            
            // Información del comprobante
            agregarInfoComprobante(document, facturaAFIP);
            
            // Cliente
            agregarCliente(document, facturaAFIP);
            
            // Items
            agregarItems(document, facturaAFIP);
            
            // Total
            agregarTotal(document, facturaAFIP);
            
            // CAE
            agregarCAE(document, facturaAFIP);
            
            // QR Code
            agregarQRCode(document, facturaAFIP);
            
            // Pie de página
            agregarPiePagina(document);
            
        } finally {
            document.close();
        }
    }
    
    /**
     * Genera el PDF y retorna los bytes
     */
    public byte[] generarTicketPDFBytes(FacturaAFIP facturaAFIP) throws Exception {
        // Tamaño de ticket: 8cm de ancho (aproximadamente 226 puntos)
        float anchoTicket = 226f; // 8cm
        float altoTicket = 800f; // Alto suficiente para el contenido
        Document document = new Document(new Rectangle(anchoTicket, altoTicket));
        document.setMargins(15, 15, 15, 15);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();
        
        try {
            agregarInfoEmpresa(document);
            agregarInfoComprobante(document, facturaAFIP);
            agregarCliente(document, facturaAFIP);
            agregarItems(document, facturaAFIP);
            agregarTotal(document, facturaAFIP);
            agregarCAE(document, facturaAFIP);
            agregarQRCode(document, facturaAFIP);
            agregarPiePagina(document);
        } finally {
            document.close();
        }
        
        return baos.toByteArray();
    }
    
    private void agregarInfoEmpresa(Document document) throws DocumentException {
        AfipProperties.Empresa empresa = afipProperties.getEmpresa();
        Paragraph p = new Paragraph();
        p.setFont(FONT_NORMAL);
        p.add(new Chunk("Razón social: " + empresa.getRazonSocial() + "\n", FONT_NORMAL));
        p.add(new Chunk("Direccion: " + empresa.getDireccion() + "\n", FONT_NORMAL));
        p.add(new Chunk("C.U.I.T.: " + empresa.getCuit() + "\n", FONT_NORMAL));
        p.add(new Chunk(empresa.getCondicionIva() + "\n", FONT_NORMAL));
        p.add(new Chunk("IIBB: " + empresa.getIibb() + "\n", FONT_NORMAL));
        p.add(new Chunk("Inicio de actividad: " + empresa.getInicioActividad() + "\n", FONT_NORMAL));
        document.add(p);
        document.add(new Paragraph(" "));
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
            String cuit = afipProperties.getEmpresa().getCuit();
            if (cuit == null || cuit.isBlank()) {
                cuit = afipProperties.getCuit();
            }
            if (cuit != null && !cuit.isBlank()) {
                cuit = cuit.replace("-", "").trim();
                json.append("\"cuit\":").append(cuit).append(",");
            } else {
                throw new Exception("CUIT no configurado");
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
    
    /**
     * Agrega el pie de página al documento
     */
    private void agregarPiePagina(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new LineSeparator());
        document.add(new Paragraph(" "));
        
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.setFont(FONT_NORMAL);
        p.add(new Chunk("Gracias por su compra\n", FONT_NORMAL));
        p.add(new Chunk("Comprobante válido como factura\n", FONT_NORMAL));
        document.add(p);
    }
}

