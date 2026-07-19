package com.vida.apirest.utils;

import com.vida.apirest.servicies.afip.AfipContext;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Cliente WSAA en Java, alineado con afip_wsaa_client.java (AFIP) y wsaa-client.php.
 */
public final class AfipWsaaClient {

    private static final Logger log = LoggerFactory.getLogger(AfipWsaaClient.class);
    private static final String WSAA_HOMO = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
    private static final String WSAA_PROD = "https://wsaa.afip.gov.ar/ws/services/LoginCms";
    private static final String DST_DN_HOMO = "CN=wsaahomo,O=AFIP,C=AR,SERIALNUMBER=CUIT 33693450239";
    private static final String DST_DN_PROD = "CN=wsaa,O=AFIP,C=AR,SERIALNUMBER=CUIT 33693450239";

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private AfipWsaaClient() {
    }

    /**
     * Solicita un TA a AFIP WSAA (cliente Java, equivalente a afip_wsaa_client.java).
     * No escribe archivos; usar {@link AfipTaStorageService#generarYGuardar}.
     */
    public static String solicitarTaXml(AfipContext context, String service) throws Exception {
        Path certDir = context.certificadosDir();
        AfipCertificateLoader.AfipCredentials credentials =
                AfipCertificateLoader.resolve(certDir, context.clavePrivadaPassword());

        X509Certificate cert = credentials.certificate();
        AfipCertificateLoader.validarCertificadoParaAmbiente(cert, context.homologacion());

        String signerDn = cert.getSubjectX500Principal().getName();
        String destinationDn = context.homologacion() ? DST_DN_HOMO : DST_DN_PROD;
        String tra = buildLoginTicketRequest(service, signerDn, destinationDn);
        String cmsBase64 = firmarTra(tra, credentials);
        String taXml = llamarLoginCms(cmsBase64, context.homologacion());

        log.info("TA recibido de AFIP para servicio {} (empresa {}, emisor cert: {})",
                service, context.empresaId(), cert.getIssuerX500Principal().getName());
        return taXml;
    }

    /**
     * TRA según afip_wsaa_client.java (wsaa_client): source/destination y ventana de 1 hora.
     */
    private static String buildLoginTicketRequest(String service, String signerDn, String destinationDn) {
        Date now = new Date();
        long ticketTimeMs = 3_600_000L;
        GregorianCalendar generation = new GregorianCalendar();
        generation.setTime(now);
        GregorianCalendar expiration = new GregorianCalendar();
        expiration.setTime(new Date(now.getTime() + ticketTimeMs));

        String generationTime = formatAfipDateTime(generation);
        String expirationTime = formatAfipDateTime(expiration);
        long uniqueId = now.getTime() / 1000L;

        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<loginTicketRequest version=\"1.0\">"
                + "<header>"
                + "<source>" + signerDn + "</source>"
                + "<destination>" + destinationDn + "</destination>"
                + "<uniqueId>" + uniqueId + "</uniqueId>"
                + "<generationTime>" + generationTime + "</generationTime>"
                + "<expirationTime>" + expirationTime + "</expirationTime>"
                + "</header>"
                + "<service>" + service + "</service>"
                + "</loginTicketRequest>";
    }

    /**
     * Formato compatible con XMLGregorianCalendar del cliente oficial AFIP.
     */
    private static String formatAfipDateTime(GregorianCalendar calendar) {
        ZonedDateTime zdt = calendar.toZonedDateTime().withZoneSameInstant(ZoneOffset.UTC);
        return zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
    }

    private static String firmarTra(String tra, AfipCertificateLoader.AfipCredentials credentials) throws Exception {
        X509Certificate cert = credentials.certificate();
        PrivateKey privateKey = credentials.privateKey();
        List<X509Certificate> chain = credentials.chain();

        CMSTypedData msg = new CMSProcessableByteArray(tra.getBytes(StandardCharsets.UTF_8));
        Store<?> certStore = new JcaCertStore(chain);

        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder("SHA1withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(privateKey);

        generator.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder()
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                .build())
                        .build(signer, cert));
        generator.addCertificates(certStore);

        // AFIP WSAA requiere CMS attached (openssl -nodetach / PKCS7 sin DETACHED).
        CMSSignedData signed = generator.generate(msg, true);
        return Base64.getEncoder().encodeToString(signed.getEncoded());
    }

    private static String llamarLoginCms(String cmsBase64, boolean homologacion) throws Exception {
        String url = homologacion ? WSAA_HOMO : WSAA_PROD;
        // AFIP WSAA responde en SOAP 1.2 (igual que wsaa-client.php).
        String soapRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\" "
                + "xmlns:wsaa=\"https://wsaa.afip.gov.ar/ws/services/LoginCms\">\n"
                + "  <soapenv:Header/>\n"
                + "  <soapenv:Body>\n"
                + "    <wsaa:loginCms>\n"
                + "      <wsaa:in0>" + cmsBase64 + "</wsaa:in0>\n"
                + "    </wsaa:loginCms>\n"
                + "  </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        if (conn instanceof HttpsURLConnection https) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            }, new java.security.SecureRandom());
            https.setSSLSocketFactory(sslContext.getSocketFactory());
            if (homologacion) {
                https.setHostnameVerifier((hostname, session) -> true);
            }
        }

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(soapRequest.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        java.io.InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            throw new IllegalStateException("WSAA sin respuesta (HTTP " + code + ")");
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        String body = response.toString();
        if (esTaYaVigente(body)) {
            throw new AfipTaAlreadyAuthenticatedException(
                    "AFIP ya tiene un token vigente para este certificado y servicio.");
        }
        if (body.contains("faultstring") || body.contains("Fault")) {
            throw new IllegalStateException(traducirError(body));
        }
        return extraerTaXml(body);
    }

    private static boolean esTaYaVigente(String body) {
        return body.contains("coe.alreadyAuthenticated")
                || body.contains("ya posee un TA valido")
                || body.contains("ya posee un TA válido");
    }

    private static String extraerTaXml(String soapResponse) throws Exception {
        String inner = extraerContenidoLoginCmsReturn(soapResponse);
        inner = desescaparEntidadesSoap(inner);

        if (AFIPTokenLoader.looksLikeTaXml(inner)) {
            return AFIPTokenLoader.sanitizeXml(inner);
        }

        try {
            String decoded = new String(
                    Base64.getDecoder().decode(inner.replaceAll("\\s+", "")),
                    StandardCharsets.UTF_8);
            decoded = desescaparEntidadesSoap(decoded.trim());
            if (AFIPTokenLoader.looksLikeTaXml(decoded)) {
                return AFIPTokenLoader.sanitizeXml(decoded);
            }
        } catch (IllegalArgumentException ignored) {
            // No era base64; se valida abajo.
        }

        throw new IllegalStateException("Respuesta WSAA sin TA XML válido");
    }

    private static String extraerContenidoLoginCmsReturn(String soapResponse) {
        int start = soapResponse.indexOf("loginCmsReturn>");
        if (start < 0) {
            throw new IllegalStateException("Respuesta WSAA inválida: " + recortar(soapResponse));
        }
        start += "loginCmsReturn>".length();

        int end = soapResponse.indexOf("</loginCmsReturn>", start);
        if (end < 0) {
            int close = soapResponse.indexOf("</", start);
            while (close >= 0) {
                int gt = soapResponse.indexOf('>', close);
                if (gt > close) {
                    String tag = soapResponse.substring(close + 2, gt);
                    if (tag.endsWith("loginCmsReturn")) {
                        end = close;
                        break;
                    }
                }
                close = soapResponse.indexOf("</", close + 1);
            }
        }
        if (end < 0) {
            throw new IllegalStateException("Respuesta WSAA inválida: " + recortar(soapResponse));
        }
        return soapResponse.substring(start, end).trim();
    }

    /**
     * Producción devuelve el TA escapado como {@code &lt;?xml ...&gt;} dentro del SOAP.
     */
    private static String desescaparEntidadesSoap(String content) {
        if (content == null || !content.contains("&")) {
            return content;
        }
        return content
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    private static String recortar(String text) {
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }

    private static String traducirError(String salida) {
        String fault = extraerFaultString(salida);
        if (salida.contains("coe.alreadyAuthenticated") || salida.contains("ya posee un TA valido")) {
            return "AFIP ya tiene un token vigente para este certificado. Se reutilizará el TA.xml local si existe.";
        }
        if (salida.contains("cms.cert.untrusted") || fault.contains("AC de confianza")) {
            return "Certificado no reconocido por AFIP en este ambiente. "
                    + "En Homologación usá el certificado de testing (emisor «Computadores Test»); "
                    + "en Producción, el de «Computadores». Verificá también que la carpeta tenga "
                    + "el certificado emitido por AFIP (no el CSR). Detalle: " + fault;
        }
        if (salida.contains("cms.sign.invalid") || salida.contains("Firma inv")) {
            return "Firma CMS rechazada por AFIP. Verificá que certificado.crt y MiClavePrivada.key "
                    + "correspondan al mismo certificado, que no estén vencidos y que sean del ambiente "
                    + "correcto. Detalle: " + fault;
        }
        if (salida.contains("Computador no autorizado") || salida.contains("notAuthorized")) {
            return "El certificado AFIP no está autorizado para este servicio. "
                    + "Verificá en AFIP → Administrador de Relaciones → Web Services.";
        }
        if (salida.contains("Servicio informado inexistente") || salida.contains("wsn.notFound")) {
            return "Servicio WSAA inválido o no habilitado para el certificado.";
        }
        return "Error WSAA: " + fault;
    }

    private static String extraerFaultString(String salida) {
        for (String tag : new String[]{"<faultstring>", "<soapenv:Text", "<Text"}) {
            int start = salida.indexOf(tag);
            if (start < 0) {
                continue;
            }
            int contentStart = salida.indexOf('>', start);
            if (contentStart < 0) {
                continue;
            }
            contentStart++;
            int end = salida.indexOf('<', contentStart);
            if (end > contentStart) {
                return decodificarXml(salida.substring(contentStart, end).trim());
            }
        }
        return salida.length() > 300 ? salida.substring(0, 300) + "..." : salida;
    }

    private static String decodificarXml(String text) {
        return text
                .replace("&#xE1;", "á")
                .replace("&#xED;", "í")
                .replace("&#xF3;", "ó")
                .replace("&#xFA;", "ú");
    }
}
