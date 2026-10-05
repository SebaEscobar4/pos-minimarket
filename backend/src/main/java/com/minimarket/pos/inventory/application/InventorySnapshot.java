package com.minimarket.pos.inventory.application;

import com.minimarket.pos.inventory.domain.InventoryMovement;
import java.util.UUID;

public record InventorySnapshot(
        UUID productId,
        String productCode,
        String productName,
        int currentBalance,
        long version,
        InventoryPage<InventoryMovement> movements) {}
