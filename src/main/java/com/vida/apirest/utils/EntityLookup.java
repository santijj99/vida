package com.vida.apirest.utils;

import com.vida.apirest.exception.ResourceNotFoundException;

import java.util.Optional;

public final class EntityLookup {

    private EntityLookup() {
    }

    public static <T> T require(Optional<T> optional, String message) {
        return optional.orElseThrow(() -> new ResourceNotFoundException(message));
    }

    public static <T> T require(Optional<T> optional, String entityName, Object id) {
        return optional.orElseThrow(() -> new ResourceNotFoundException(entityName + " no encontrado" + formatId(id)));
    }

    private static String formatId(Object id) {
        return id != null ? " con id: " + id : "";
    }
}
