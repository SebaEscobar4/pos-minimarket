package com.minimarket.pos.cash.domain;

import com.minimarket.pos.shared.domain.Money;
import java.time.Instant;
import java.util.UUID;

public record CashMovement(
        UUID id,
        UUID cashSessionId,
        CashMovementType type,
        CashMovementCategory category,
        Money amount,
        String reason,
        String reference,
        UUID actorId,
        String actorDisplayName,
        Instant occurredAt) {}
