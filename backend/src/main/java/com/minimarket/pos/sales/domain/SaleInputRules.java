package com.minimarket.pos.sales.domain;

import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;

public final class SaleInputRules {

    public static final int MAXIMUM_LINES = 100;
    public static final int MAXIMUM_QUANTITY = 1_000_000;
    public static final int MAXIMUM_PAGE_SIZE = 100;

    private SaleInputRules() {}

    public static void validateIntent(UUID idempotencyKey, Collection<?> lines, PaymentMethod method) {
        if (idempotencyKey == null) {
            throw validation("La clave de confirmación es obligatoria.");
        }
        if (lines == null || lines.isEmpty() || lines.size() > MAXIMUM_LINES) {
            throw validation("La venta debe contener entre 1 y 100 productos distintos.");
        }
        if (method == null) {
            throw validation("El método de pago es obligatorio.");
        }
    }

    public static int quantity(Integer value) {
        if (value == null || value < 1 || value > MAXIMUM_QUANTITY) {
            throw validation("La cantidad debe ser un entero entre 1 y 1000000.");
        }
        return value;
    }

    public static Payment payment(
            PaymentMethod method, BigDecimal cashReceived, Money total, java.time.Instant occurredAt) {
        if (method == PaymentMethod.CASH) {
            CashRounding.Result rounding = CashRounding.apply(total);
            Money received;
            try {
                received = Money.of(cashReceived);
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw validation("El efectivo recibido debe ser un monto CLP entero válido.");
            }
            if (received.amount().compareTo(rounding.cashPayable().amount()) < 0) {
                throw new ApplicationException(
                        ProblemType.CONFLICT,
                        "El efectivo recibido es menor que el total efectivo redondeado.");
            }
            return new Payment(
                    UUID.randomUUID(),
                    method,
                    total,
                    rounding.cashPayable(),
                    rounding.adjustment(),
                    received,
                    Money.of(received.amount().subtract(rounding.cashPayable().amount())),
                    occurredAt);
        }
        if (cashReceived != null) {
            throw validation("El efectivo recibido solo corresponde a pagos en efectivo.");
        }
        return new Payment(UUID.randomUUID(), method, total, null, null, null, null, occurredAt);
    }

    public static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw validation("La página debe ser positiva y su tamaño debe estar entre 1 y 100.");
        }
    }

    private static ApplicationException validation(String message) {
        return new ApplicationException(ProblemType.VALIDATION, message);
    }
}
