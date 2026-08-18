package com.vida.apirest.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Ventana fija en memoria (por instancia). Alcanza para un POS; no sustituye un bucket distribuido.
 */
@Component
public class AuthRateLimiter {

    public static final long WINDOW_MS = 15 * 60 * 1000L;

    private final ConcurrentHashMap<String, Counter> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int max) {
        if (key == null || key.isBlank() || max <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        Counter updated = buckets.compute(key, (k, prev) -> {
            if (prev == null || now - prev.windowStart >= WINDOW_MS) {
                return new Counter(1, now);
            }
            return new Counter(prev.count + 1, prev.windowStart);
        });
        pruneIfNeeded();
        return updated.count <= max;
    }

    public void reset(String key) {
        if (key != null) {
            buckets.remove(key);
        }
    }

    private void pruneIfNeeded() {
        if (buckets.size() < 8_000) {
            return;
        }
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> now - e.getValue().windowStart >= WINDOW_MS);
    }

    private record Counter(int count, long windowStart) {
    }
}
