package com.minimarket.pos.inventory.application;

import java.util.UUID;

public interface InventoryBalanceInitializer {

    void initializeAtZero(UUID productId);
}
