package com.vida.apirest.utils;

import java.util.Optional;
import java.util.function.Supplier;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static boolean defaultActivo(Boolean activo) {
        return activo != null ? activo : true;
    }

    public static void assertUnique(Supplier<Optional<?>> existing, String message) {
        if (existing.get().isPresent()) {
            throw new RuntimeException(message);
        }
    }
}
