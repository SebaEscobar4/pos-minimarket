package com.minimarket.pos.inventory.domain;

import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;

public final class InventoryInputRules {

    public static final int MAXIMUM_MOVEMENT_QUANTITY = 1_000_000;
    public static final int MAXIMUM_BALANCE = 100_000_000;
    public static final int MAXIMUM_REASON_LENGTH = 500;
    public static final int MAXIMUM_REFERENCE_LENGTH = 100;
    public static final int MAXIMUM_PAGE_SIZE = 100;

    private InventoryInputRules() {}

    public static int quantity(Integer value) {
        if (value == null || value < 1 || value > MAXIMUM_MOVEMENT_QUANTITY) {
            throw validation("La cantidad debe ser un entero entre 1 y 1000000.");
        }
        return value;
    }

    public static String requiredReason(String value) {
        String reason = optionalText(value, MAXIMUM_REASON_LENGTH, "motivo");
        if (reason == null) {
            throw validation("El motivo del ajuste es obligatorio.");
        }
        return reason;
    }

    public static Evidence entryEvidence(String reasonValue, String referenceValue) {
        String reason = optionalText(reasonValue, MAXIMUM_REASON_LENGTH, "motivo");
        String reference = optionalText(referenceValue, MAXIMUM_REFERENCE_LENGTH, "referencia");
        if (reason == null && reference == null) {
            throw validation("La entrada requiere un motivo o una referencia.");
        }
        return new Evidence(reason, reference);
    }

    public static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw validation("La página debe ser positiva y su tamaño debe estar entre 1 y 100.");
        }
    }

    private static String optionalText(String value, int maximumLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maximumLength) {
            throw validation("El " + fieldName + " supera el máximo de " + maximumLength + " caracteres.");
        }
        return normalized;
    }

    private static ApplicationException validation(String message) {
        return new ApplicationException(ProblemType.VALIDATION, message);
    }

    public record Evidence(String reason, String reference) {}
}
