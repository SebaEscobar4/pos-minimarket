package com.minimarket.pos.cash.application;

import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashCloseResult(
        UUID sessionId,
        UUID closedBy,
        String closedByDisplayName,
        Instant closedAt,
        Money openingAmount,
        Money expectedCash,
        Money countedCash,
        BigDecimal difference,
        CashTotals totals) {}
