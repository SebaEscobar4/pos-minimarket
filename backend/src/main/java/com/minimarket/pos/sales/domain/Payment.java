package com.minimarket.pos.sales.domain;

import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Payment(
        UUID id,
        PaymentMethod method,
        Money amount,
        Money cashPayable,
        BigDecimal roundingAdjustment,
        Money cashReceived,
        Money change,
        Instant occurredAt) {}
