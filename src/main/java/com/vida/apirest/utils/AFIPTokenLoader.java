package com.vida.apirest.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public final class AFIPTokenLoader {

    private AFIPTokenLoader() {
    }

    public static TokenSign loadFromXml(String path) throws Exception {
        InputStream inputStream = openStream(path);
        try (inputStream) {
            return parseDocument(inputStream);
        }
    }

    public static TokenSign loadFromXmlContent(String xml) throws Exception {
        String sanitized = sanitizeXml(xml);
        try (InputStream inputStream = new java.io.ByteArrayInputStream(
                sanitized.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            return parseDocument(inputStream);
        }
    }

    public static boolean looksLikeTaXml(String xml) {
        if (xml == null || xml.isBlank()) {
            return false;
        }
        String sanitized = sanitizeXml(xml);
        return sanitized.startsWith("<?xml")
                || sanitized.startsWith("<loginTicketResponse")
                || sanitized.startsWith("<credentials");
    }

    public static String sanitizeXml(String xml) {
        if (xml == null) {
            return "";
        }
        String trimmed = xml.stripLeading();
        if (trimmed.startsWith("\uFEFF")) {
            trimmed = trimmed.substring(1).stripLeading();
        }
        return trimmed;
    }

    /**
     * Verifica que el TA interno (SSO) corresponda al servicio WSAA solicitado.
     */
    public static boolean tokenEsParaServicio(TokenSign tokenSign, String service) {
        if (tokenSign == null || service == null || service.isBlank()) {
            return false;
        }
        try {
            String sso = new String(Base64.getDecoder().decode(tokenSign.getToken().replaceAll("\\s+", "")),
                    StandardCharsets.UTF_8);
            return sso.contains("service=\"" + service + "\"")
                    || sso.contains("service>" + service + "<");
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean tokenEsParaServicio(String taXml, String service) throws Exception {
        if (!looksLikeTaXml(taXml)) {
            return false;
        }
        return tokenEsParaServicio(loadFromXmlContent(taXml), service);
    }

    private static TokenSign parseDocument(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(inputStream);
        doc.getDocumentElement().normalize();

        String token = readTag(doc, "token");
        String sign = readTag(doc, "sign");
        Date expiration = parseExpiration(readTag(doc, "expirationTime"));

        if (token == null || sign == null) {
            throw new IllegalStateException("El archivo TA.xml no contiene token o sign");
        }

        return new TokenSign(token.trim(), sign.trim(), expiration);
    }

    private static InputStream openStream(String path) throws Exception {
        if (path.startsWith("classpath:")) {
            Resource resource = new ClassPathResource(path.substring("classpath:".length()));
            if (!resource.exists()) {
                throw new IllegalStateException("No se encontró el token en classpath: " + path);
            }
            return resource.getInputStream();
        }

        File file = new File(path);
        if (!file.exists()) {
            throw new IllegalStateException("No se encontró el token en: " + path);
        }
        return new java.io.FileInputStream(file);
    }

    private static String readTag(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private static Date parseExpiration(String expStr) {
        if (expStr == null || expStr.isBlank()) {
            return null;
        }

        String expStrFixed = expStr.trim();
        int signIndex = Math.max(expStrFixed.lastIndexOf("+"), expStrFixed.lastIndexOf("-"));
        if (signIndex > 10) {
            String beforeTz = expStrFixed.substring(0, signIndex);
            String tzPart = expStrFixed.substring(signIndex);
            if (tzPart.contains(":") && tzPart.length() == 6) {
                tzPart = tzPart.substring(0, 3) + tzPart.substring(4);
            }
            expStrFixed = beforeTz + tzPart;
        }

        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(expStrFixed);
        } catch (Exception e1) {
            try {
                if (expStrFixed.contains(".")) {
                    int dotIndex = expStrFixed.indexOf(".");
                    int tzIndex = expStrFixed.indexOf("-", dotIndex);
                    if (tzIndex == -1) {
                        tzIndex = expStrFixed.indexOf("+", dotIndex);
                    }
                    if (tzIndex > 0) {
                        expStrFixed = expStrFixed.substring(0, dotIndex) + expStrFixed.substring(tzIndex);
                    }
                }
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(expStrFixed);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    public static class TokenSign {
        private final String token;
        private final String sign;
        private final Date expiration;

        public TokenSign(String token, String sign, Date expiration) {
            this.token = token;
            this.sign = sign;
            this.expiration = expiration;
        }

        public String getToken() {
            return token;
        }

        public String getSign() {
            return sign;
        }

        public Date getExpiration() {
            return expiration;
        }

        public boolean isValid() {
            if (expiration == null) {
                return true;
            }
            return expiration.getTime() > System.currentTimeMillis() + 60_000L;
        }
    }
}
