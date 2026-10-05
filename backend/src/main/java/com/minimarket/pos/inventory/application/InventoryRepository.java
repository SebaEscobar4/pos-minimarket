package com.minimarket.pos.inventory.application;

import com.minimarket.pos.inventory.domain.InventoryMovement;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {

    Optional<LockedBalance> lockBalance(UUID productId);

    boolean updateBalance(UUID productId, long expectedVersion, int resultingBalance);

    void append(InventoryMovement movement);

    Optional<BalanceView> findBalance(UUID productId);

    InventoryPage<InventoryMovement> findMovements(UUID productId, int page, int size);

    record LockedBalance(int quantity, long version) {}

    record BalanceView(
            UUID productId, String productCode, String productName, int quantity, long version) {}
}
