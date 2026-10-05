package com.minimarket.pos.cash.domain;

public enum CashMovementType {
    CASH_SALE(1),
    MANUAL_INCOME(1),
    MANUAL_WITHDRAWAL(-1),
    CASH_REFUND(-1);

    private final int direction;

    CashMovementType(int direction) {
        this.direction = direction;
    }

    public int direction() {
        return direction;
    }
}
