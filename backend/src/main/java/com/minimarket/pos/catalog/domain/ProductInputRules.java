package com.minimarket.pos.catalog.domain;

import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ProductInputRules {

    public static final int MAXIMUM_CODE_LENGTH = 64;
    public static final int MAXIMUM_NAME_LENGTH = 150;
    public static final int MAXIMUM_STOCK = 1_000_000;
    public static final int MAXIMUM_PAGE_SIZE = 100;
    public static final BigDecimal MAXIMUM_MONEY = new BigDecimal("9999999999.99");

    private static final Pattern CODE_PATTERN = Pattern.compile("[-A-Za-z0-9._/]{1,64}");
    private static final Pattern DIACRITIC_PATTERN = Pattern.compile("\\p{M}+");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private ProductInputRules() {}

    public static String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String code = value.strip();
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw validation(
                    "El código admite entre 1 y 64 letras, números o caracteres - . _ /. ");
        }
        return code;
    }

    public static String normalizeName(String value) {
        if (value == null) {
            throw invalidName();
        }
        String name = WHITESPACE_PATTERN.matcher(value.strip()).replaceAll(" ");
        if (name.isEmpty() || name.length() > MAXIMUM_NAME_LENGTH) {
            throw invalidName();
        }
        return name;
    }

    public static String searchName(String value) {
        String name = normalizeName(value).toLowerCase(Locale.ROOT);
        return DIACRITIC_PATTERN.matcher(Normalizer.normalize(name, Normalizer.Form.NFD))
                .replaceAll("");
    }

    public static BigDecimal money(String fieldName, BigDecimal value) {
        if (value == null
                || value.signum() < 0
                || value.compareTo(MAXIMUM_MONEY) > 0
                || value.stripTrailingZeros().scale() > 2) {
            throw validation(fieldName + " debe ser un monto entre 0 y 9999999999,99 con hasta 2 decimales.");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static int minimumStock(Integer value) {
        if (value == null || value < 0 || value > MAXIMUM_STOCK) {
            throw validation("El stock mínimo debe ser un entero entre 0 y 1000000.");
        }
        return value;
    }

    public static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw validation("La página debe ser positiva y su tamaño debe estar entre 1 y 100.");
        }
    }

    private static ApplicationException invalidName() {
        return validation("El nombre del producto es obligatorio y admite hasta 150 caracteres.");
    }

    private static ApplicationException validation(String message) {
        return new ApplicationException(ProblemType.VALIDATION, message);
    }
}
