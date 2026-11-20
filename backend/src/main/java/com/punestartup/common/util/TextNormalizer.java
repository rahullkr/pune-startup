package com.punestartup.common.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TextNormalizer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern SLUG_INVALID = Pattern.compile("[^a-z0-9]+");
    private static final Pattern SLUG_HYPHENS = Pattern.compile("(^-+|-+$)");

    private TextNormalizer() {
    }

    public static String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        return WHITESPACE.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll(" ");
    }

    public static String slugify(String value) {
        if (value == null) {
            return null;
        }
        String normalized = normalizeName(value);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        String slug = SLUG_INVALID.matcher(normalized).replaceAll("-");
        slug = SLUG_HYPHENS.matcher(slug).replaceAll("");
        return slug.isEmpty() ? null : slug;
    }

    public static String escapeLike(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public static String likePattern(String value) {
        String escaped = escapeLike(value);
        return escaped == null ? null : "%" + escaped + "%";
    }
}
