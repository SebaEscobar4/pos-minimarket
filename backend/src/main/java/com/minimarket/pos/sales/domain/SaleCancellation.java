package com.minimarket.pos.sales.domain;

import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaleCancellation(
        UUID id,
        UUID saleId,
        PaymentMethod refundMethod,
        Money amount,
        Money cashPayable,
        BigDecimal roundingAdjustment,
        String reason,
        UUID actorId,
        String actorDisplayName,
        Instant occurredAt) {}
