package com.minimarket.pos.cash.domain;

import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;

public final class CashInputRules {

    public static final int MAXIMUM_REASON_LENGTH = 500;
    public static final int MAXIMUM_PAGE_SIZE = 100;

    private CashInputRules() {}

    public static Money openingAmount(BigDecimal value) {
        return nonNegativeAmount(value, "El monto inicial");
    }

    public static Money countedCash(BigDecimal value) {
        return nonNegativeAmount(value, "El efectivo contado");
    }

    public static Money movementAmount(BigDecimal value) {
        Money amount = nonNegativeAmount(value, "El monto del movimiento");
        if (amount.amount().signum() == 0) {
            throw validation("El monto del movimiento debe ser mayor que cero.");
        }
        return amount;
    }

    public static String requiredReason(String value) {
        if (value == null) {
            throw validation("El motivo es obligatorio y admite hasta 500 caracteres.");
        }
        String reason = value.strip();
        if (reason.isEmpty() || reason.length() > MAXIMUM_REASON_LENGTH) {
            throw validation("El motivo es obligatorio y admite hasta 500 caracteres.");
        }
        return reason;
    }

    public static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw validation("La página debe ser positiva y su tamaño debe estar entre 1 y 100.");
        }
    }

    private static Money nonNegativeAmount(BigDecimal value, String fieldName) {
        if (value == null
                || value.signum() < 0
                || value.compareTo(Money.MAXIMUM_AMOUNT) > 0
                || value.stripTrailingZeros().scale() > 0) {
            throw validation(
                    fieldName + " debe ser un entero CLP entre 0 y 9007199254740991.");
        }
        return Money.of(value);
    }

    private static ApplicationException validation(String message) {
        return new ApplicationException(ProblemType.VALIDATION, message);
    }
}
