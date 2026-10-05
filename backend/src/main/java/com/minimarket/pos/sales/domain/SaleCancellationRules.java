package com.minimarket.pos.sales.domain;

import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import com.minimarket.pos.shared.domain.Money;
import java.time.Instant;
import java.util.UUID;

public final class SaleCancellationRules {

    public static final int MAXIMUM_REASON_LENGTH = 500;

    private SaleCancellationRules() {}

    public static SaleCancellation create(
            UUID saleId,
            PaymentMethod refundMethod,
            Money exactTotal,
            String reasonValue,
            UUID actorId,
            String actorDisplayName,
            Instant occurredAt) {
        if (saleId == null || refundMethod == null || exactTotal == null || actorId == null
                || actorDisplayName == null || actorDisplayName.isBlank() || occurredAt == null) {
            throw validation("Los datos de la anulación son obligatorios.");
        }
        if (reasonValue == null || reasonValue.isBlank()) {
            throw validation("El motivo de la anulación es obligatorio.");
        }
        String reason = reasonValue.strip();
        if (reason.length() > MAXIMUM_REASON_LENGTH) {
            throw validation("El motivo de la anulación supera los 500 caracteres.");
        }
        CashRounding.Result rounding = refundMethod == PaymentMethod.CASH
                ? CashRounding.apply(exactTotal)
                : null;
        return new SaleCancellation(
                UUID.randomUUID(),
                saleId,
                refundMethod,
                exactTotal,
                rounding == null ? null : rounding.cashPayable(),
                rounding == null ? null : rounding.adjustment(),
                reason,
                actorId,
                actorDisplayName.strip(),
                occurredAt);
    }

    private static ApplicationException validation(String message) {
        return new ApplicationException(ProblemType.VALIDATION, message);
    }
}
