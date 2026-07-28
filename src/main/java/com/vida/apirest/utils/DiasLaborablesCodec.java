package com.vida.apirest.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Codifica días laborables ISO (1=lun … 7=dom) como CSV en DB, ej. {@code "1,2,3,4,5"}.
 */
public final class DiasLaborablesCodec {

    private DiasLaborablesCodec() {
    }

    /** Lun–Vie por defecto para sueldos diarios nuevos. */
    public static List<Integer> defaultLunesAViernes() {
        return List.of(1, 2, 3, 4, 5);
    }

    public static List<Integer> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        Set<Integer> out = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            try {
                int v = Integer.parseInt(t);
                if (v >= 1 && v <= 7) {
                    out.add(v);
                }
            } catch (NumberFormatException ignored) {
                // ignore token inválido
            }
        }
        return List.copyOf(out);
    }

    public static String encode(List<Integer> dias) {
        if (dias == null || dias.isEmpty()) {
            return null;
        }
        Set<Integer> clean = new LinkedHashSet<>();
        for (Integer v : dias) {
            if (v != null && v >= 1 && v <= 7) {
                clean.add(v);
            }
        }
        if (clean.isEmpty()) {
            return null;
        }
        return clean.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
    }

    public static List<Integer> normalizeOrDefaultForDia(List<Integer> dias) {
        if (dias == null || dias.isEmpty()) {
            return defaultLunesAViernes();
        }
        List<Integer> parsed = new ArrayList<>();
        for (Integer v : dias) {
            if (v != null && v >= 1 && v <= 7 && !parsed.contains(v)) {
                parsed.add(v);
            }
        }
        Collections.sort(parsed);
        return parsed.isEmpty() ? defaultLunesAViernes() : List.copyOf(parsed);
    }
}
