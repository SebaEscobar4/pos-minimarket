package com.minimarket.pos.catalog.domain;

import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;

public final class CategoryInputRules {

    public static final int MAXIMUM_NAME_LENGTH = 100;

    private CategoryInputRules() {}

    public static String normalizeName(String value) {
        if (value == null) {
            throw invalidName();
        }
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > MAXIMUM_NAME_LENGTH) {
            throw invalidName();
        }
        return normalized;
    }

    private static ApplicationException invalidName() {
        return new ApplicationException(
                ProblemType.VALIDATION,
                "El nombre de la categoría es obligatorio y admite hasta 100 caracteres.");
    }
}
