package com.minimarket.pos.inventory.domain;

import java.time.Instant;
import java.util.UUID;

public record InventoryMovement(
        UUID id,
        UUID productId,
        InventoryMovementType type,
        int quantity,
        int delta,
        int previousBalance,
        int resultingBalance,
        String reason,
        String reference,
        UUID actorId,
        String actorDisplayName,
        Instant occurredAt) {}
