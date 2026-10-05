package com.minimarket.pos.inventory.domain;

public enum InventoryMovementType {
    ENTRY(1),
    SALE_OUT(-1),
    ADJUSTMENT_IN(1),
    ADJUSTMENT_OUT(-1),
    DAMAGED(-1),
    EXPIRED(-1),
    SALE_REVERSAL(1);

    private final int direction;

    InventoryMovementType(int direction) {
        this.direction = direction;
    }

    public int deltaFor(int quantity) {
        return Math.multiplyExact(direction, quantity);
    }
}
