package com.vida.apirest.servicies.afip;

import com.vida.apirest.config.AfipProperties;
import com.vida.apirest.utils.AFIPTokenLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Consulta Padrón Alcance 13 (ws_sr_padron_a13).
 */
@Service
@RequiredArgsConstructor
public class PadronA13Service {

    private static final String SERVICE = "ws_sr_padron_a13";
    private static final String NS = "http://a13.soap.ws.server.puc.sr/";
    private static final String PADRON_URL_HOMO =
            "https://awshomo.afip.gov.ar/sr-padron/webservices/personaServiceA13";
    private static final String PADRON_URL_PROD =
            "https://aws.afip.gov.ar/sr-padron/webservices/personaServiceA13";

    private final AfipProperties afipProperties;
    private final WSAAService wsaaService;

    public DatosPadron consultarPorCuit(String cuit) throws Exception {
        AFIPTokenLoader.TokenSign tokenSign = obtenerTokenPadron();
        String soap = buildGetPersonaSoap(tokenSign, cuit);
        return parsearPersona(enviarSoap(soap));
    }

    public DatosPadron consultarPorDni(String dni) throws Exception {
        AFIPTokenLoader.TokenSign tokenSign = obtenerTokenPadron();
        String soapDoc = buildGetIdPersonaListByDocumentoSoap(tokenSign, dni);
        String respuestaDoc = enviarSoap(soapDoc);
        String cuit = extraerPrimerIdPersona(respuestaDoc);
        if (cuit == null || cuit.isBlank()) {
            throw new IllegalStateException("DNI no encontrado en el padrón AFIP");
        }
        return consultarPorCuit(cuit);
    }

    private AFIPTokenLoader.TokenSign obtenerTokenPadron() throws Exception {
        WSAAService.TokenSign token = wsaaService.obtenerTokenSign(SERVICE);
        return new AFIPTokenLoader.TokenSign(token.getToken(), token.getSign(), token.getExpiration());
    }

    private String buildGetPersonaSoap(AFIPTokenLoader.TokenSign tokenSign, String idPersona) {
        String token = tokenSign.getToken().trim().replaceAll("\\s+", "");
        String sign = tokenSign.getSign().trim().replaceAll("\\s+", "");
        String cuitRepresentada = AfipContextHolder.require().cuitSinGuiones();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:a13=\"" + NS + "\">\n"
                + "  <soapenv:Header/>\n"
                + "  <soapenv:Body>\n"
                + "    <a13:getPersona>\n"
                + "      <token>" + token + "</token>\n"
                + "      <sign>" + sign + "</sign>\n"
                + "      <cuitRepresentada>" + cuitRepresentada + "</cuitRepresentada>\n"
                + "      <idPersona>" + idPersona + "</idPersona>\n"
                + "    </a13:getPersona>\n"
                + "  </soapenv:Body>\n"
                + "</soapenv:Envelope>";
    }

    private String buildGetIdPersonaListByDocumentoSoap(AFIPTokenLoader.TokenSign tokenSign, String documento) {
        String token = tokenSign.getToken().trim().replaceAll("\\s+", "");
        String sign = tokenSign.getSign().trim().replaceAll("\\s+", "");
        String cuitRepresentada = AfipContextHolder.require().cuitSinGuiones();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:a13=\"" + NS + "\">\n"
                + "  <soapenv:Header/>\n"
                + "  <soapenv:Body>\n"
                + "    <a13:getIdPersonaListByDocumento>\n"
                + "      <token>" + token + "</token>\n"
                + "      <sign>" + sign + "</sign>\n"
                + "      <cuitRepresentada>" + cuitRepresentada + "</cuitRepresentada>\n"
                + "      <documento>" + documento + "</documento>\n"
                + "    </a13:getIdPersonaListByDocumento>\n"
                + "  </soapenv:Body>\n"
                + "</soapenv:Envelope>";
    }

    private String enviarSoap(String soapRequest) throws Exception {
        String url = afipProperties.isHomologacion() ? PADRON_URL_HOMO : PADRON_URL_PROD;
        URL padronUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) padronUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "\"\"");
        conn.setDoOutput(true);

        if (afipProperties.isHomologacion() && conn instanceof HttpsURLConnection https) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            }, new java.security.SecureRandom());
            https.setSSLSocketFactory(sslContext.getSocketFactory());
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(soapRequest.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        java.io.InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            throw new IllegalStateException("Padrón A13 sin respuesta (HTTP " + code + ")");
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        if (code >= 400) {
            throw new IllegalStateException("Padrón A13 HTTP " + code + ": " + response);
        }

        return response.toString();
    }

    private String extraerPrimerIdPersona(String xml) {
        if (xml.contains("<faultstring>") || xml.contains("soapenv:Fault")) {
            String fault = extraerTag(xml, "faultstring");
            throw new IllegalStateException(fault != null ? fault : "Error en consulta padrón AFIP");
        }
        return extraerTag(xml, "idPersona");
    }

    private DatosPadron parsearPersona(String xml) {
        if (xml.contains("<faultstring>") || xml.contains("soapenv:Fault")) {
            String fault = extraerTag(xml, "faultstring");
            throw new IllegalStateException(fault != null ? fault : "Error en consulta padrón AFIP");
        }

        String personaXml = extraerBloque(xml, "persona");
        if (personaXml == null) {
            personaXml = xml;
        }

        String razonSocial = extraerTag(personaXml, "razonSocial");
        if (razonSocial == null || razonSocial.isBlank()) {
            String apellido = extraerTag(personaXml, "apellido");
            String nombre = extraerTag(personaXml, "nombre");
            if (apellido != null || nombre != null) {
                razonSocial = ((apellido != null ? apellido : "") + " " + (nombre != null ? nombre : "")).trim();
            }
        }

        String domicilio = extraerDomicilioFiscal(personaXml);
        Integer condicionIva = inferirCondicionIva(personaXml);

        if (razonSocial == null || razonSocial.isBlank()) {
            throw new IllegalStateException("AFIP no devolvió nombre para el documento consultado");
        }

        return new DatosPadron(razonSocial.trim(), domicilio, condicionIva);
    }

    private String extraerDomicilioFiscal(String xml) {
        Pattern block = Pattern.compile("<domicilio>(.*?)</domicilio>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = block.matcher(xml);
        String fiscal = null;
        String legal = null;
        String first = null;
        while (matcher.find()) {
            String bloque = matcher.group(1);
            String direccion = extraerTag(bloque, "direccion");
            if (direccion == null || direccion.isBlank()) {
                direccion = construirDomicilio(
                        extraerTag(bloque, "calle"),
                        extraerTag(bloque, "numero"),
                        extraerTag(bloque, "localidad"));
            }
            if (direccion == null || direccion.isBlank()) {
                continue;
            }
            if (first == null) {
                first = direccion;
            }
            String tipo = extraerTag(bloque, "tipoDomicilio");
            if (tipo != null && tipo.toUpperCase().contains("FISCAL")) {
                fiscal = direccion;
            } else if (tipo != null && (tipo.toUpperCase().contains("LEGAL") || tipo.toUpperCase().contains("REAL"))) {
                legal = direccion;
            }
        }
        if (fiscal != null) {
            return fiscal;
        }
        if (legal != null) {
            return legal;
        }
        String direccion = extraerTag(xml, "direccion");
        return direccion != null ? direccion : first;
    }

    private Integer inferirCondicionIva(String xml) {
        String lower = xml.toLowerCase();
        if (lower.contains("monotributo") || contieneImpuestoVigente(xml, "32")) {
            return 6;
        }
        if (contieneImpuestoVigente(xml, "30")) {
            return 1;
        }
        if (lower.contains("exento")) {
            return 4;
        }
        return null;
    }

    private boolean contieneImpuestoVigente(String xml, String idImpuesto) {
        Pattern impuesto = Pattern.compile("<impuesto>(.*?)</impuesto>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = impuesto.matcher(xml);
        while (matcher.find()) {
            String bloque = matcher.group(1);
            String id = extraerTag(bloque, "idImpuesto");
            String vigente = extraerTag(bloque, "esVigente");
            if (idImpuesto.equals(id) && (vigente == null || "S".equalsIgnoreCase(vigente))) {
                return true;
            }
        }
        return false;
    }

    private String extraerBloque(String xml, String tag) {
        Pattern pattern = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String construirDomicilio(String calle, String numero, String localidad) {
        StringBuilder sb = new StringBuilder();
        if (calle != null && !calle.isBlank()) {
            sb.append(calle.trim());
        }
        if (numero != null && !numero.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(numero.trim());
        }
        if (localidad != null && !localidad.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(localidad.trim());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String extraerTag(String xml, String tag) {
        Pattern pattern = Pattern.compile("<" + tag + ">([^<]*)</" + tag + ">", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    public record DatosPadron(String razonSocial, String domicilio, Integer condicionIVAReceptorId) {
    }
}
