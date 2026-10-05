package com.minimarket.pos.cash.domain;

import com.minimarket.pos.shared.domain.Money;
import java.time.Instant;
import java.util.UUID;

public record CashSession(
        UUID id,
        UUID openedBy,
        String openedByDisplayName,
        Instant openedAt,
        Money openingAmount,
        CashSessionStatus status) {}
