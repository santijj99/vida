package com.vida.apirest.utils;

import java.util.Locale;
import java.util.Set;

public final class FileUploadUtils {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private FileUploadUtils() {
    }

    public static String safeProfileFileName(String originalFilename) {
        return "perfil" + safeImageExtension(originalFilename);
    }

    public static String safeImageExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".jpg";
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension) ? extension : ".jpg";
    }
}
