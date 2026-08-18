package com.vida.apirest.servicies.licencia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vida.apirest.config.LicenciaProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LicenciaServerClient {

    private final LicenciaProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ValidacionRemotaResult validar(String codigoLicencia, String uuidDispositivo) {
        return validar(codigoLicencia, uuidDispositivo, null);
    }

    public ValidacionRemotaResult validar(
            String codigoLicencia,
            String uuidDispositivo,
            String nombreDispositivo
    ) {
        String base = trimSlash(properties.getServerUrl());
        RestClient client = RestClient.builder().baseUrl(base).build();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("codigoLicencia", codigoLicencia);
        payload.put("uuidDispositivo", uuidDispositivo);
        if (nombreDispositivo != null && !nombreDispositivo.isBlank()) {
            payload.put("nombreDispositivo", nombreDispositivo);
        }

        try {
            String body = client.post()
                    .uri("/api/v1/licencias/validar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                data = root;
            }

            return ValidacionRemotaResult.ok(
                    text(data, "estado", "ACTIVA"),
                    date(data, "fechaVencimiento"),
                    text(data, "empresaNombre", null),
                    text(data, "planNombre", null),
                    intVal(data, "cantidadMaximaDispositivos"),
                    intVal(data, "cantidadMaximaSucursales"),
                    text(data, "host", null),
                    intVal(data, "puerto"),
                    text(data, "databaseName", null),
                    text(data, "username", null),
                    text(data, "passwordEncriptada", null),
                    data.has("ssl") ? data.get("ssl").asBoolean(true) : Boolean.TRUE
            );
        } catch (RestClientResponseException ex) {
            String message = "Licencia rechazada";
            String code = "LICENCIA_RECHAZADA";
            try {
                JsonNode err = objectMapper.readTree(ex.getResponseBodyAsString());
                if (err.hasNonNull("message")) {
                    message = err.get("message").asText();
                }
                if (err.hasNonNull("errorCode")) {
                    code = err.get("errorCode").asText();
                }
            } catch (Exception ignored) {
                // keep defaults
            }
            return ValidacionRemotaResult.rechazada(code, message);
        } catch (Exception ex) {
            log.warn("No se pudo contactar el servidor de licencias: {}", ex.getMessage());
            return ValidacionRemotaResult.inalcanzable(ex.getMessage());
        }
    }

    public ConsumoSoporteResult consumirSoporte(String codigoLicencia, String token) {
        String base = trimSlash(properties.getServerUrl());
        RestClient client = RestClient.builder().baseUrl(base).build();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("codigoLicencia", codigoLicencia);
        payload.put("token", token);

        try {
            String body = client.post()
                    .uri("/api/v1/licencias/soporte/consumir")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                data = root;
            }
            if (!data.path("valido").asBoolean(false)) {
                return ConsumoSoporteResult.rechazado("SOPORTE_INVALIDO", "Ticket de soporte inválido");
            }
            return ConsumoSoporteResult.ok(
                    data.path("ticketId").isNumber() ? data.get("ticketId").asLong() : null,
                    text(data, "codigoLicencia", codigoLicencia),
                    text(data, "empresaNombre", null),
                    text(data, "operadorEmail", null),
                    intVal(data, "duracionHoras"),
                    instant(data, "expiraEn")
            );
        } catch (RestClientResponseException ex) {
            String message = "Ticket de soporte rechazado";
            String code = "SOPORTE_RECHAZADO";
            try {
                JsonNode err = objectMapper.readTree(ex.getResponseBodyAsString());
                if (err.hasNonNull("message")) {
                    message = err.get("message").asText();
                }
                if (err.hasNonNull("errorCode")) {
                    code = err.get("errorCode").asText();
                }
            } catch (Exception ignored) {
                // keep defaults
            }
            return ConsumoSoporteResult.rechazado(code, message);
        } catch (Exception ex) {
            log.warn("No se pudo contactar el servidor de licencias para soporte: {}", ex.getMessage());
            return ConsumoSoporteResult.inalcanzable(ex.getMessage());
        }
    }

    private static java.time.Instant instant(JsonNode node, String field) {
        String raw = text(node, field, null);
        if (raw == null) {
            return null;
        }
        try {
            return java.time.Instant.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8080";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return fallback;
        }
        String t = v.asText();
        return t == null || t.isBlank() ? fallback : t;
    }

    private static Integer intVal(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || !v.canConvertToInt()) {
            return null;
        }
        return v.asInt();
    }

    private static LocalDate date(JsonNode node, String field) {
        String raw = text(node, field, null);
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    @Getter
    public static class ValidacionRemotaResult {
        private final boolean alcanzable;
        private final boolean valida;
        private final String estado;
        private final String codigoError;
        private final String mensaje;
        private final LocalDate fechaVencimiento;
        private final String empresaNombre;
        private final String planNombre;
        private final Integer cantidadMaximaDispositivos;
        private final Integer cantidadMaximaSucursales;
        private final String host;
        private final Integer puerto;
        private final String databaseName;
        private final String username;
        private final String passwordEncriptada;
        private final Boolean ssl;

        private ValidacionRemotaResult(
                boolean alcanzable,
                boolean valida,
                String estado,
                String codigoError,
                String mensaje,
                LocalDate fechaVencimiento,
                String empresaNombre,
                String planNombre,
                Integer cantidadMaximaDispositivos,
                Integer cantidadMaximaSucursales,
                String host,
                Integer puerto,
                String databaseName,
                String username,
                String passwordEncriptada,
                Boolean ssl
        ) {
            this.alcanzable = alcanzable;
            this.valida = valida;
            this.estado = estado;
            this.codigoError = codigoError;
            this.mensaje = mensaje;
            this.fechaVencimiento = fechaVencimiento;
            this.empresaNombre = empresaNombre;
            this.planNombre = planNombre;
            this.cantidadMaximaDispositivos = cantidadMaximaDispositivos;
            this.cantidadMaximaSucursales = cantidadMaximaSucursales;
            this.host = host;
            this.puerto = puerto;
            this.databaseName = databaseName;
            this.username = username;
            this.passwordEncriptada = passwordEncriptada;
            this.ssl = ssl;
        }

        public static ValidacionRemotaResult ok(
                String estado,
                LocalDate fechaVencimiento,
                String empresaNombre,
                String planNombre,
                Integer maxDisp,
                Integer maxSuc,
                String host,
                Integer puerto,
                String databaseName,
                String username,
                String passwordEncriptada,
                Boolean ssl
        ) {
            return new ValidacionRemotaResult(
                    true, true, estado, null, "Licencia válida",
                    fechaVencimiento, empresaNombre, planNombre, maxDisp, maxSuc,
                    host, puerto, databaseName, username, passwordEncriptada, ssl
            );
        }

        public static ValidacionRemotaResult rechazada(String code, String message) {
            return new ValidacionRemotaResult(
                    true, false, "INVALIDA", code, message,
                    null, null, null, null, null,
                    null, null, null, null, null, null
            );
        }

        public static ValidacionRemotaResult inalcanzable(String detail) {
            return new ValidacionRemotaResult(
                    false, false, "DESCONOCIDO", "SERVIDOR_INALCANZABLE",
                    detail == null || detail.isBlank()
                            ? "No se pudo contactar el servidor de licencias"
                            : detail,
                    null, null, null, null, null,
                    null, null, null, null, null, null
            );
        }

        public boolean hasConnection() {
            return host != null && !host.isBlank()
                    && databaseName != null && !databaseName.isBlank()
                    && username != null && !username.isBlank()
                    && passwordEncriptada != null && !passwordEncriptada.isBlank()
                    && puerto != null;
        }
    }

    @Getter
    public static class ConsumoSoporteResult {
        private final boolean alcanzable;
        private final boolean valido;
        private final String codigoError;
        private final String mensaje;
        private final Long ticketId;
        private final String codigoLicencia;
        private final String empresaNombre;
        private final String operadorEmail;
        private final Integer duracionHoras;
        private final java.time.Instant expiraEn;

        private ConsumoSoporteResult(
                boolean alcanzable,
                boolean valido,
                String codigoError,
                String mensaje,
                Long ticketId,
                String codigoLicencia,
                String empresaNombre,
                String operadorEmail,
                Integer duracionHoras,
                java.time.Instant expiraEn
        ) {
            this.alcanzable = alcanzable;
            this.valido = valido;
            this.codigoError = codigoError;
            this.mensaje = mensaje;
            this.ticketId = ticketId;
            this.codigoLicencia = codigoLicencia;
            this.empresaNombre = empresaNombre;
            this.operadorEmail = operadorEmail;
            this.duracionHoras = duracionHoras;
            this.expiraEn = expiraEn;
        }

        public static ConsumoSoporteResult ok(
                Long ticketId,
                String codigoLicencia,
                String empresaNombre,
                String operadorEmail,
                Integer duracionHoras,
                java.time.Instant expiraEn
        ) {
            return new ConsumoSoporteResult(
                    true, true, null, "Soporte autorizado",
                    ticketId, codigoLicencia, empresaNombre, operadorEmail, duracionHoras, expiraEn
            );
        }

        public static ConsumoSoporteResult rechazado(String code, String message) {
            return new ConsumoSoporteResult(
                    true, false, code, message,
                    null, null, null, null, null, null
            );
        }

        public static ConsumoSoporteResult inalcanzable(String detail) {
            return new ConsumoSoporteResult(
                    false, false, "SERVIDOR_INALCANZABLE",
                    detail == null || detail.isBlank()
                            ? "No se pudo contactar el servidor de licencias"
                            : detail,
                    null, null, null, null, null, null
            );
        }
    }
}
