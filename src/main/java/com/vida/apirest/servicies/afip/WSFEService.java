package com.vida.apirest.servicies.afip;

import com.vida.apirest.config.AfipProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Service
@RequiredArgsConstructor
@Slf4j
public class WSFEService {

    private static final String WSFE_URL_HOMOLOGACION = "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";
    private static final String WSFE_URL_PRODUCCION = "https://servicios1.afip.gov.ar/wsfev1/service.asmx";

    private final WSAAService wsaaService;
    private final AfipProperties afipProperties;
    
    /**
     * Obtiene el último comprobante autorizado
     */
    public Long obtenerUltimoComprobanteAutorizado(Integer ptoVta, Integer cbteTipo) throws Exception {
        WSAAService.TokenSign tokenSign = wsaaService.obtenerTokenSign();
        
        // Limpiar token y sign (eliminar espacios y saltos de línea)
        String token = tokenSign.getToken().trim().replaceAll("\\s+", "");
        String sign = tokenSign.getSign().trim().replaceAll("\\s+", "");
        
        String soapRequest = crearSoapRequestFECompUltimoAutorizado(
            token,
            sign,
            ptoVta,
            cbteTipo
        );
        
        String respuesta = enviarSoapRequest(soapRequest, "FECompUltimoAutorizado");
        
        return parsearUltimoComprobante(respuesta);
    }
    
    /**
     * Solicita CAE para una factura
     */
    public FECAEResponse solicitarCAE(FECAERequest request) throws Exception {
        WSAAService.TokenSign tokenSign = wsaaService.obtenerTokenSign();
        
        // Limpiar token y sign (eliminar espacios y saltos de línea)
        String token = tokenSign.getToken().trim().replaceAll("\\s+", "");
        String sign = tokenSign.getSign().trim().replaceAll("\\s+", "");

        log.debug("FECAESolicitar token/sign listos (longitudes {} / {})", token.length(), sign.length());
        
        String soapRequest = crearSoapRequestFECAESolicitar(
            token,
            sign,
            request
        );
        
        String respuesta = enviarSoapRequest(soapRequest, "FECAESolicitar");
        log.debug("FECAESolicitar respuesta recibida ({} chars)", respuesta.length());

        return parsearFECAEResponse(respuesta);
    }
    
    /**
     * Crea el SOAP request para FECompUltimoAutorizado
     */
    private String crearSoapRequestFECompUltimoAutorizado(String token, String sign, 
                                                           Integer ptoVta, Integer cbteTipo) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">\n" +
            "  <soapenv:Header/>\n" +
            "  <soapenv:Body>\n" +
            "    <ar:FECompUltimoAutorizado>\n" +
            "      <ar:Auth>\n" +
            "        <ar:Token>" + token + "</ar:Token>\n" +
            "        <ar:Sign>" + sign + "</ar:Sign>\n" +
            "        <ar:Cuit>" + obtenerCuit() + "</ar:Cuit>\n" +
            "      </ar:Auth>\n" +
            "      <ar:PtoVta>" + ptoVta + "</ar:PtoVta>\n" +
            "      <ar:CbteTipo>" + cbteTipo + "</ar:CbteTipo>\n" +
            "    </ar:FECompUltimoAutorizado>\n" +
            "  </soapenv:Body>\n" +
            "</soapenv:Envelope>";
    }
    
    /**
     * Crea el SOAP request para FECAESolicitar
     */
    private String crearSoapRequestFECAESolicitar(String token, String sign, FECAERequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ar=\"http://ar.gov.afip.dif.FEV1/\">\n");
        sb.append("  <soapenv:Header/>\n");
        sb.append("  <soapenv:Body>\n");
        sb.append("    <ar:FECAESolicitar>\n");
        sb.append("      <ar:Auth>\n");
        sb.append("        <ar:Token>").append(token).append("</ar:Token>\n");
        sb.append("        <ar:Sign>").append(sign).append("</ar:Sign>\n");
        sb.append("        <ar:Cuit>").append(obtenerCuit()).append("</ar:Cuit>\n");
        sb.append("      </ar:Auth>\n");
        sb.append("      <ar:FeCAEReq>\n");
        sb.append("        <ar:FeCabReq>\n");
        sb.append("          <ar:CantReg>").append(request.getItems().size()).append("</ar:CantReg>\n");
        sb.append("          <ar:PtoVta>").append(request.getPtoVta()).append("</ar:PtoVta>\n");
        sb.append("          <ar:CbteTipo>").append(request.getCbteTipo()).append("</ar:CbteTipo>\n");
        sb.append("        </ar:FeCabReq>\n");
        sb.append("        <ar:FeDetReq>\n");
        
        for (FECAERequestItem item : request.getItems()) {
            sb.append("          <ar:FECAEDetRequest>\n");
            sb.append("            <ar:Concepto>").append(item.getConcepto()).append("</ar:Concepto>\n");
            sb.append("            <ar:DocTipo>").append(item.getDocTipo()).append("</ar:DocTipo>\n");
            sb.append("            <ar:DocNro>").append(item.getDocNro()).append("</ar:DocNro>\n");
            sb.append("            <ar:CbteDesde>").append(item.getCbteDesde()).append("</ar:CbteDesde>\n");
            sb.append("            <ar:CbteHasta>").append(item.getCbteHasta()).append("</ar:CbteHasta>\n");
            sb.append("            <ar:CbteFch>").append(item.getCbteFch()).append("</ar:CbteFch>\n");
            sb.append("            <ar:ImpTotal>").append(item.getImpTotal()).append("</ar:ImpTotal>\n");
            sb.append("            <ar:ImpTotConc>").append(item.getImpTotConc()).append("</ar:ImpTotConc>\n");
            sb.append("            <ar:ImpNeto>").append(item.getImpNeto()).append("</ar:ImpNeto>\n");
            sb.append("            <ar:ImpOpEx>").append(item.getImpOpEx()).append("</ar:ImpOpEx>\n");
            sb.append("            <ar:ImpTrib>").append(item.getImpTrib()).append("</ar:ImpTrib>\n");
            sb.append("            <ar:ImpIVA>").append(item.getImpIVA()).append("</ar:ImpIVA>\n");
            sb.append("            <ar:MonId>").append(item.getMonId()).append("</ar:MonId>\n");
            sb.append("            <ar:MonCotiz>").append(item.getMonCotiz()).append("</ar:MonCotiz>\n");
            sb.append("            <ar:CondicionIVAReceptorId>").append(item.getCondicionIVAReceptorId()).append("</ar:CondicionIVAReceptorId>\n");
            
            // Tributos
            if (item.getTributos() != null && !item.getTributos().isEmpty()) {
                sb.append("            <ar:Tributos>\n");
                for (FECAERequestTributo tributo : item.getTributos()) {
                    sb.append("              <ar:Tributo>\n");
                    sb.append("                <ar:Id>").append(tributo.getId()).append("</ar:Id>\n");
                    sb.append("                <ar:Desc>").append(escapeXml(tributo.getDesc())).append("</ar:Desc>\n");
                    sb.append("                <ar:BaseImp>").append(tributo.getBaseImp()).append("</ar:BaseImp>\n");
                    sb.append("                <ar:Alic>").append(tributo.getAlic()).append("</ar:Alic>\n");
                    sb.append("                <ar:Importe>").append(tributo.getImporte()).append("</ar:Importe>\n");
                    sb.append("              </ar:Tributo>\n");
                }
                sb.append("            </ar:Tributos>\n");
            }
            
            // IVA
            if (item.getIvas() != null && !item.getIvas().isEmpty()) {
                sb.append("            <ar:Iva>\n");
                for (FECAERequestIva iva : item.getIvas()) {
                    sb.append("              <ar:AlicIva>\n");
                    sb.append("                <ar:Id>").append(iva.getId()).append("</ar:Id>\n");
                    sb.append("                <ar:BaseImp>").append(iva.getBaseImp()).append("</ar:BaseImp>\n");
                    sb.append("                <ar:Importe>").append(iva.getImporte()).append("</ar:Importe>\n");
                    sb.append("              </ar:AlicIva>\n");
                }
                sb.append("            </ar:Iva>\n");
            }
            
            sb.append("          </ar:FECAEDetRequest>\n");
        }
        
        sb.append("        </ar:FeDetReq>\n");
        sb.append("      </ar:FeCAEReq>\n");
        sb.append("    </ar:FECAESolicitar>\n");
        sb.append("  </soapenv:Body>\n");
        sb.append("</soapenv:Envelope>");
        
        return sb.toString();
    }
    
    private String escapeXml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                 .replace("<", "&lt;")
                 .replace(">", "&gt;")
                 .replace("\"", "&quot;")
                 .replace("'", "&apos;");
    }
    
    /**
     * Envía el request SOAP al servicio
     */
    private String enviarSoapRequest(String soapRequest, String operation) throws Exception {
        String url = afipProperties.isHomologacion() ? WSFE_URL_HOMOLOGACION : WSFE_URL_PRODUCCION;
        
        URL wsfeUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) wsfeUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "http://ar.gov.afip.dif.FEV1/" + operation);
        conn.setDoOutput(true);
        
        // Configurar SSL para homologación
        if (afipProperties.isHomologacion()) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            }, new java.security.SecureRandom());
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        }
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(soapRequest.getBytes(StandardCharsets.UTF_8));
        }
        
        // Verificar código de respuesta
        int responseCode = conn.getResponseCode();
        log.debug("WSFE {} HTTP {}", operation, responseCode);
        
        StringBuilder response = new StringBuilder();
        
        // Si hay error, leer del error stream
        if (responseCode >= 400) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            log.warn("WSFE {} falló HTTP {} ({} chars)", operation, responseCode, response.length());
            throw new Exception("Error HTTP " + responseCode + " al llamar " + operation);
        }
        
        // Leer respuesta exitosa
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        return response.toString();
    }
    
    /**
     * Parsea la respuesta de FECompUltimoAutorizado
     */
    private Long parsearUltimoComprobante(String respuesta) throws Exception {
        int inicio = respuesta.indexOf("<CbteNro>");
        int fin = respuesta.indexOf("</CbteNro>");
        
        if (inicio == -1 || fin == -1) {
            throw new Exception("No se pudo obtener el último comprobante AFIP");
        }
        
        String numero = respuesta.substring(inicio + 9, fin).trim();
        return Long.parseLong(numero);
    }
    
    /**
     * Parsea la respuesta de FECAESolicitar
     * La respuesta SOAP de AFIP tiene esta estructura:
     * <FECAESolicitarResponse>
     *   <FECAESolicitarResult>
     *     <FeCabResp>...</FeCabResp>
     *     <FeDetResp>
     *       <FECAEDetResponse>
     *         <Resultado>A</Resultado>
     *         <CAE>...</CAE>
     *         ...
     *       </FECAEDetResponse>
     *     </FeDetResp>
     *   </FECAESolicitarResult>
     * </FECAESolicitarResponse>
     */
    private FECAEResponse parsearFECAEResponse(String respuesta) throws Exception {
        FECAEResponse response = new FECAEResponse();

        int detResponseInicio = respuesta.indexOf("<FECAEDetResponse");
        if (detResponseInicio == -1) {
            detResponseInicio = respuesta.indexOf("<FECAEDetResponse>");
        }
        
        String contenidoDetResponse = respuesta;
        if (detResponseInicio != -1) {
            // Encontrar el cierre de FECAEDetResponse
            int detResponseFin = respuesta.indexOf("</FECAEDetResponse>", detResponseInicio);
            if (detResponseFin != -1) {
                contenidoDetResponse = respuesta.substring(detResponseInicio, detResponseFin);
            }
        }
        
        // Buscar Resultado con diferentes patrones (con y sin namespace)
        String[] patronesResultado = {
            "<Resultado>", "</Resultado>",
            "<ar:Resultado>", "</ar:Resultado>",
            "Resultado>", "</Resultado>",
            "Resultado ", "</Resultado>"
        };
        
        String resultado = buscarCampoEnXml(contenidoDetResponse, "Resultado", patronesResultado);
        if (resultado == null || resultado.isEmpty()) {
            // Intentar buscar en toda la respuesta
            resultado = buscarCampoEnXml(respuesta, "Resultado", patronesResultado);
        }
        
        if (resultado != null && !resultado.isEmpty()) {
            response.setResultado(resultado);
            log.debug("FECAESolicitar Resultado={}", resultado);
        } else {
            log.warn("FECAESolicitar sin tag Resultado ({} chars)", contenidoDetResponse.length());
        }
        
        // Buscar CAE
        String cae = buscarCampoEnXml(contenidoDetResponse, "CAE", new String[]{
            "<CAE>", "</CAE>",
            "<ar:CAE>", "</ar:CAE>",
            "CAE>", "</CAE>"
        });
        if (cae == null || cae.isEmpty()) {
            cae = buscarCampoEnXml(respuesta, "CAE", new String[]{"<CAE>", "</CAE>", "<ar:CAE>", "</ar:CAE>"});
        }
        if (cae != null && !cae.isEmpty()) {
            response.setCae(cae);
            log.debug("FECAESolicitar CAE presente");
        } else {
            log.debug("FECAESolicitar sin CAE");
        }
        
        // Buscar CAEFchVto
        String caeFchVto = buscarCampoEnXml(contenidoDetResponse, "CAEFchVto", new String[]{
            "<CAEFchVto>", "</CAEFchVto>",
            "<ar:CAEFchVto>", "</ar:CAEFchVto>",
            "CAEFchVto>", "</CAEFchVto>"
        });
        if (caeFchVto == null || caeFchVto.isEmpty()) {
            caeFchVto = buscarCampoEnXml(respuesta, "CAEFchVto", new String[]{"<CAEFchVto>", "</CAEFchVto>"});
        }
        if (caeFchVto != null && !caeFchVto.isEmpty()) {
            response.setCaeFchVto(caeFchVto);
        }
        
        // Buscar Motivos
        String motivos = buscarCampoEnXml(contenidoDetResponse, "Motivos", new String[]{
            "<Motivos>", "</Motivos>",
            "<ar:Motivos>", "</ar:Motivos>",
            "Motivos>", "</Motivos>"
        });
        if (motivos == null || motivos.isEmpty()) {
            motivos = buscarCampoEnXml(respuesta, "Motivos", new String[]{"<Motivos>", "</Motivos>"});
        }
        if (motivos != null && !motivos.isEmpty()) {
            response.setMotivos(motivos);
        }
        
        // Buscar Observaciones
        String observaciones = buscarCampoEnXml(contenidoDetResponse, "Observaciones", new String[]{
            "<Observaciones>", "</Observaciones>",
            "<ar:Observaciones>", "</ar:Observaciones>",
            "Observaciones>", "</Observaciones>"
        });
        if (observaciones == null || observaciones.isEmpty()) {
            observaciones = buscarCampoEnXml(respuesta, "Observaciones", new String[]{"<Observaciones>", "</Observaciones>"});
        }
        if (observaciones != null && !observaciones.isEmpty()) {
            response.setObservaciones(observaciones);
        }
        
        // Buscar errores en la respuesta (pueden estar en <Errors><Err>)
        if (respuesta.contains("<Errors>") || respuesta.contains("<Err>")) {
            log.debug("FECAESolicitar incluye bloque Errors");
            
            // Buscar todos los errores
            String erroresCompletos = "";
            int errorInicio = respuesta.indexOf("<Errors>");
            if (errorInicio != -1) {
                int errorFin = respuesta.indexOf("</Errors>", errorInicio);
                if (errorFin != -1) {
                    String seccionErrores = respuesta.substring(errorInicio, errorFin);
                    
                    // Buscar cada error individual
                    int pos = 0;
                    while ((pos = seccionErrores.indexOf("<Err>", pos)) != -1) {
                        int errFin = seccionErrores.indexOf("</Err>", pos);
                        if (errFin != -1) {
                            String errorIndividual = seccionErrores.substring(pos, errFin);
                            
                            // Extraer código
                            String codigo = buscarCampoEnXml(errorIndividual, "Code", new String[]{
                                "<Code>", "</Code>", "Code>", "</Code>"
                            });
                            
                            // Extraer mensaje
                            String mensaje = buscarCampoEnXml(errorIndividual, "Msg", new String[]{
                                "<Msg>", "</Msg>", "Msg>", "</Msg>"
                            });
                            
                            if (codigo != null && mensaje != null) {
                                String errorCompleto = "Error " + codigo + ": " + mensaje;
                                if (!erroresCompletos.isEmpty()) {
                                    erroresCompletos += " | ";
                                }
                                erroresCompletos += errorCompleto;
                                log.warn("AFIP rechazó FECAESolicitar: {}", errorCompleto);
                            }
                            
                            pos = errFin;
                        } else {
                            break;
                        }
                    }
                }
            }
            
            if (!erroresCompletos.isEmpty()) {
                response.setMotivos(erroresCompletos);
                response.setResultado("R"); // Rechazado por error
            }
        }
        
        // Si hay errores SOAP, buscarlos
        if (respuesta.contains("<soap:Fault>") || respuesta.contains("<faultstring>") || respuesta.contains("Fault>")) {
            String error = buscarCampoEnXml(respuesta, "faultstring", new String[]{
                "<faultstring>", "</faultstring>",
                "<soap:faultstring>", "</soap:faultstring>",
                "faultstring>", "</faultstring>"
            });
            if (error != null) {
                log.warn("AFIP SOAP fault: {}", error);
                if (response.getMotivos() == null || response.getMotivos().isEmpty()) {
                    response.setMotivos("Error SOAP: " + error);
                }
                response.setResultado("R");
            }
        }
        
        // Si aún no hay resultado y no hay errores, mostrar más de la respuesta para debugging
        if (response.getResultado() == null && (response.getMotivos() == null || response.getMotivos().isEmpty())) {
            log.warn("FECAESolicitar sin Resultado ni motivos ({} chars)", respuesta.length());
        }

        return response;
    }
    
    /**
     * Busca un campo en XML con diferentes patrones posibles
     */
    private String buscarCampoEnXml(String xml, String nombreCampo, String[] patrones) {
        // Intentar con cada patrón
        for (int i = 0; i < patrones.length; i += 2) {
            if (i + 1 >= patrones.length) break;
            
            String inicioTag = patrones[i];
            String finTag = patrones[i + 1];
            
            int inicio = xml.indexOf(inicioTag);
            if (inicio != -1) {
                int fin = xml.indexOf(finTag, inicio);
                if (fin != -1) {
                    String valor = xml.substring(inicio + inicioTag.length(), fin).trim();
                    if (!valor.isEmpty()) {
                        return valor;
                    }
                }
            }
        }
        
        // Si no se encuentra con los patrones, buscar de forma más flexible
        // Buscar cualquier tag que contenga el nombre del campo (con o sin namespace)
        // Patrón: <algo:nombreCampo> o <nombreCampo> o nombreCampo>
        String[] patronesFlexibles = {
            "<" + nombreCampo + ">",
            "</" + nombreCampo + ">",
            "<" + nombreCampo + " ",
            " " + nombreCampo + ">",
            ":" + nombreCampo + ">"
        };
        
        for (String patron : patronesFlexibles) {
            int pos = xml.indexOf(patron);
            if (pos != -1) {
                // Encontrar el inicio del tag
                int tagInicio = xml.lastIndexOf("<", pos);
                if (tagInicio != -1) {
                    // Encontrar el fin del tag de apertura
                    int tagFinApertura = xml.indexOf(">", tagInicio);
                    if (tagFinApertura != -1) {
                        String tagApertura = xml.substring(tagInicio, tagFinApertura + 1);
                        // Construir el tag de cierre
                        String tagCierre = "</" + tagApertura.substring(1);
                        // Si el tag de apertura tiene namespace, el cierre también
                        if (tagApertura.contains(":")) {
                            int colonPos = tagApertura.indexOf(":");
                            String namespace = tagApertura.substring(1, colonPos);
                            tagCierre = "</" + namespace + ":" + nombreCampo + ">";
                        } else {
                            tagCierre = "</" + nombreCampo + ">";
                        }
                        
                        int finFlexible = xml.indexOf(tagCierre, tagFinApertura);
                        if (finFlexible != -1) {
                            String valor = xml.substring(tagFinApertura + 1, finFlexible).trim();
                            if (!valor.isEmpty()) {
                                return valor;
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    private String obtenerCuit() {
        AfipContext context = AfipContextHolder.get();
        if (context != null && context.cuitSinGuiones() != null && !context.cuitSinGuiones().isBlank()) {
            return context.cuitSinGuiones();
        }
        throw new IllegalStateException("No hay CUIT AFIP en el contexto de la empresa");
    }
    
    // Clases de request/response
    public static class FECAERequest {
        private Integer ptoVta;
        private Integer cbteTipo;
        private List<FECAERequestItem> items = new ArrayList<>();
        
        public Integer getPtoVta() { return ptoVta; }
        public void setPtoVta(Integer ptoVta) { this.ptoVta = ptoVta; }
        
        public Integer getCbteTipo() { return cbteTipo; }
        public void setCbteTipo(Integer cbteTipo) { this.cbteTipo = cbteTipo; }
        
        public List<FECAERequestItem> getItems() { return items; }
        public void setItems(List<FECAERequestItem> items) { this.items = items; }
    }
    
    public static class FECAERequestItem {
        private Integer concepto;
        private Integer docTipo;
        private String docNro;
        private Long cbteDesde;
        private Long cbteHasta;
        private String cbteFch;
        private BigDecimal impTotal;
        private BigDecimal impTotConc;
        private BigDecimal impNeto;
        private BigDecimal impOpEx;
        private BigDecimal impTrib;
        private BigDecimal impIVA;
        private String monId = "PES";
        private BigDecimal monCotiz = BigDecimal.ONE;
        private Integer condicionIVAReceptorId;
        private List<FECAERequestTributo> tributos = new ArrayList<>();
        private List<FECAERequestIva> ivas = new ArrayList<>();
        
        // Getters y setters
        public Integer getConcepto() { return concepto; }
        public void setConcepto(Integer concepto) { this.concepto = concepto; }
        
        public Integer getDocTipo() { return docTipo; }
        public void setDocTipo(Integer docTipo) { this.docTipo = docTipo; }
        
        public String getDocNro() { return docNro; }
        public void setDocNro(String docNro) { this.docNro = docNro; }
        
        public Long getCbteDesde() { return cbteDesde; }
        public void setCbteDesde(Long cbteDesde) { this.cbteDesde = cbteDesde; }
        
        public Long getCbteHasta() { return cbteHasta; }
        public void setCbteHasta(Long cbteHasta) { this.cbteHasta = cbteHasta; }
        
        public String getCbteFch() { return cbteFch; }
        public void setCbteFch(String cbteFch) { this.cbteFch = cbteFch; }
        
        public BigDecimal getImpTotal() { return impTotal; }
        public void setImpTotal(BigDecimal impTotal) { this.impTotal = impTotal; }
        
        public BigDecimal getImpTotConc() { return impTotConc; }
        public void setImpTotConc(BigDecimal impTotConc) { this.impTotConc = impTotConc; }
        
        public BigDecimal getImpNeto() { return impNeto; }
        public void setImpNeto(BigDecimal impNeto) { this.impNeto = impNeto; }
        
        public BigDecimal getImpOpEx() { return impOpEx; }
        public void setImpOpEx(BigDecimal impOpEx) { this.impOpEx = impOpEx; }
        
        public BigDecimal getImpTrib() { return impTrib; }
        public void setImpTrib(BigDecimal impTrib) { this.impTrib = impTrib; }
        
        public BigDecimal getImpIVA() { return impIVA; }
        public void setImpIVA(BigDecimal impIVA) { this.impIVA = impIVA; }
        
        public String getMonId() { return monId; }
        public void setMonId(String monId) { this.monId = monId; }
        
        public BigDecimal getMonCotiz() { return monCotiz; }
        public void setMonCotiz(BigDecimal monCotiz) { this.monCotiz = monCotiz; }
        
        public Integer getCondicionIVAReceptorId() { return condicionIVAReceptorId; }
        public void setCondicionIVAReceptorId(Integer condicionIVAReceptorId) { this.condicionIVAReceptorId = condicionIVAReceptorId; }
        
        public List<FECAERequestTributo> getTributos() { return tributos; }
        public void setTributos(List<FECAERequestTributo> tributos) { this.tributos = tributos; }
        
        public List<FECAERequestIva> getIvas() { return ivas; }
        public void setIvas(List<FECAERequestIva> ivas) { this.ivas = ivas; }
    }
    
    public static class FECAERequestTributo {
        private Integer id;
        private String desc;
        private BigDecimal baseImp;
        private BigDecimal alic;
        private BigDecimal importe;
        
        // Getters y setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        
        public String getDesc() { return desc; }
        public void setDesc(String desc) { this.desc = desc; }
        
        public BigDecimal getBaseImp() { return baseImp; }
        public void setBaseImp(BigDecimal baseImp) { this.baseImp = baseImp; }
        
        public BigDecimal getAlic() { return alic; }
        public void setAlic(BigDecimal alic) { this.alic = alic; }
        
        public BigDecimal getImporte() { return importe; }
        public void setImporte(BigDecimal importe) { this.importe = importe; }
    }
    
    public static class FECAERequestIva {
        private Integer id;
        private BigDecimal baseImp;
        private BigDecimal importe;
        
        // Getters y setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        
        public BigDecimal getBaseImp() { return baseImp; }
        public void setBaseImp(BigDecimal baseImp) { this.baseImp = baseImp; }
        
        public BigDecimal getImporte() { return importe; }
        public void setImporte(BigDecimal importe) { this.importe = importe; }
    }
    
    public static class FECAEResponse {
        private String resultado; // A=Aprobado, R=Rechazado, O=Observado
        private String cae;
        private String caeFchVto;
        private String motivos;
        private String observaciones;
        
        // Getters y setters
        public String getResultado() { return resultado; }
        public void setResultado(String resultado) { this.resultado = resultado; }
        
        public String getCae() { return cae; }
        public void setCae(String cae) { this.cae = cae; }
        
        public String getCaeFchVto() { return caeFchVto; }
        public void setCaeFchVto(String caeFchVto) { this.caeFchVto = caeFchVto; }
        
        public String getMotivos() { return motivos; }
        public void setMotivos(String motivos) { this.motivos = motivos; }
        
        public String getObservaciones() { return observaciones; }
        public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    }
}

