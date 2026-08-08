package com.vida.apirest.config;

import com.vida.apirest.dto.common.ApiErrorResponse;
import com.vida.apirest.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex) {
        logClientError(ex.getStatus(), ex.getMessage());
        return buildResponse(ex.getMessage(), ex.getStatus());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        logClientError(HttpStatus.BAD_REQUEST, ex.getMessage());
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = resolverIntegrityMessage(ex);
        logClientError(HttpStatus.BAD_REQUEST, message);
        return buildResponse(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(RuntimeException ex) {
        HttpStatus status = resolveRuntimeStatus(ex.getMessage());
        logClientError(status, ex.getMessage());
        return buildResponse(ex.getMessage(), status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex) {
        log.error("Error inesperado: {}", ex.getClass().getSimpleName(), ex);
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return buildResponse(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void logClientError(HttpStatus status, String message) {
        if (status.is5xxServerError()) {
            log.error("Error de aplicación ({}): {}", status.value(), message);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Solicitud rechazada ({}): {}", status.value(), message);
        }
    }

    private HttpStatus resolveRuntimeStatus(String message) {
        if (message == null) {
            return HttpStatus.BAD_REQUEST;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("no encontrad") || lower.contains("not found")) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private String resolverIntegrityMessage(DataIntegrityViolationException ex) {
        String raw = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        if (raw == null) {
            return "No se pudo guardar: datos inconsistentes";
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("uk_taxon_articulo") || (lower.contains("taxon") && lower.contains("articulo_id"))) {
            return "La subcategoría y una clasificación usan la misma etiqueta. Quitá el duplicado o dejá solo una.";
        }
        if (lower.contains("uk_variante_articulo_color_talle")
                || (lower.contains("color_id") && lower.contains("talle_id") && lower.contains("articulo_id"))) {
            return "Ya existe una variante con ese talle y color en este artículo";
        }
        if (lower.contains("uk_variante_codigo_barras") || lower.contains("codigo_barras")) {
            return "El código de barras ya está registrado en otra variante";
        }
        if (lower.contains("ix_articulo_codigo") || (lower.contains("(codigo)=") && lower.contains("articulo"))) {
            return "Ya existe un artículo con ese código base";
        }
        if (lower.contains("proveedor_id") && lower.contains("tercero")) {
            return "El proveedor no es válido. Reinicie la API para aplicar la migración de base de datos.";
        }
        if (lower.contains("proveedor_id")) {
            return "El proveedor seleccionado no existe o no es válido";
        }
        if (lower.contains("(celular)=") || (lower.contains("celular") && lower.contains("unique"))) {
            return "El celular ya está registrado en otro usuario";
        }
        if (lower.contains("(email)=") || (lower.contains("email") && lower.contains("unique"))) {
            return "El correo ya está registrado en otro usuario";
        }
        if (lower.contains("(usuario)=") || (lower.contains("usuario") && lower.contains("unique"))) {
            return "El nombre de usuario ya está en uso";
        }
        return "No se pudo guardar: datos inconsistentes";
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(String message, HttpStatus status) {
        String safeMessage = message != null ? message : status.getReasonPhrase();
        if (status.is5xxServerError()) {
            safeMessage = "Error interno del servidor";
        }
        return ResponseEntity.status(status).body(new ApiErrorResponse(safeMessage, status.value()));
    }
}
