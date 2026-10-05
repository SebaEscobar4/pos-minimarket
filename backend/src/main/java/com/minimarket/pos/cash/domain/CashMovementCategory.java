package com.minimarket.pos.cash.domain;

public enum CashMovementCategory {
    CASH_REPLENISHMENT(ManualCashDirection.INCOME),
    OTHER_INCOME(ManualCashDirection.INCOME),
    SUPPLIER_PAYMENT(ManualCashDirection.WITHDRAWAL),
    OPERATING_EXPENSE(ManualCashDirection.WITHDRAWAL),
    SAFE_DROP(ManualCashDirection.WITHDRAWAL),
    OTHER_WITHDRAWAL(ManualCashDirection.WITHDRAWAL);

    private final ManualCashDirection direction;

    CashMovementCategory(ManualCashDirection direction) {
        this.direction = direction;
    }

    public ManualCashDirection direction() {
        return direction;
    }
}
