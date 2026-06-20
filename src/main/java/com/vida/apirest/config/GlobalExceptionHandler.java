package com.vida.apirest.config;

import com.vida.apirest.dto.common.ApiErrorResponse;
import com.vida.apirest.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private ResponseEntity<ApiErrorResponse> buildResponse(String message, HttpStatus status) {
        String safeMessage = message != null ? message : status.getReasonPhrase();
        if (status.is5xxServerError()) {
            safeMessage = "Error interno del servidor";
        }
        return ResponseEntity.status(status).body(new ApiErrorResponse(safeMessage, status.value()));
    }
}
