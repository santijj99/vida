package com.vida.apirest.utils;

public final class PaginationUtils {

    public static final int DEFAULT_PAGE_SIZE = 15;
    public static final int MAX_PAGE_SIZE = 100;

    private PaginationUtils() {
    }

    public record PageParams(int page, int size) {
    }

    public static PageParams normalize(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE));
        return new PageParams(safePage, safeSize);
    }

    public static int normalizePage(int page) {
        return Math.max(0, page);
    }

    public static int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    public static String normalizeQuery(String q) {
        return q == null ? "" : q.trim();
    }
}
