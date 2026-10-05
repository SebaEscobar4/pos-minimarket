package com.minimarket.pos.cash.application;

import com.minimarket.pos.shared.domain.Money;

public record CashTotals(
        Money cashSales,
        Money manualIncome,
        Money manualWithdrawals,
        Money cashRefunds,
        Money cardSales,
        Money transferSales) {}
