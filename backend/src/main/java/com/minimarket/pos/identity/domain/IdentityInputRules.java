package com.minimarket.pos.identity.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class IdentityInputRules {

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{3,50}");

    private IdentityInputRules() {}

    public static String normalizeUsername(String value) {
        if (value == null) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        String normalized = Normalizer.normalize(value.strip(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "El nombre de usuario debe tener entre 3 y 50 caracteres: letras minúsculas, números, punto, guion o guion bajo.");
        }
        return normalized;
    }

    public static String requireDisplayName(String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.length() < 2 || normalized.length() > 100) {
            throw new IllegalArgumentException("El nombre visible debe tener entre 2 y 100 caracteres.");
        }
        return normalized;
    }

    public static String requirePassword(String value, int minimumLength, int maximumLength) {
        if (value == null || value.length() < minimumLength || value.length() > maximumLength) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener entre " + minimumLength + " y " + maximumLength + " caracteres.");
        }
        return value;
    }
}
