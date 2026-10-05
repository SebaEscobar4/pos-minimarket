package com.minimarket.pos.cash.application;

import com.minimarket.pos.cash.domain.CashMovement;
import com.minimarket.pos.cash.domain.CashSession;
import com.minimarket.pos.shared.domain.Money;

public record CashSummary(
        CashSession session,
        CashTotals totals,
        Money expectedCash,
        CashPage<CashMovement> movements) {}
