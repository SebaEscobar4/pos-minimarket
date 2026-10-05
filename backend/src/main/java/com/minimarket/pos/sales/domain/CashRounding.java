package com.minimarket.pos.sales.domain;

import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;

public final class CashRounding {

    private static final BigDecimal TEN = BigDecimal.TEN;

    private CashRounding() {}

    public static Result apply(Money exactTotal) {
        int lastDigit = exactTotal.amount().remainder(TEN).intValueExact();
        int adjustment = switch (lastDigit) {
            case 1, 2, 3, 4, 5 -> -lastDigit;
            case 6, 7, 8, 9 -> 10 - lastDigit;
            default -> 0;
        };
        BigDecimal roundingAdjustment = BigDecimal.valueOf(adjustment);
        Money cashPayable = Money.of(exactTotal.amount().add(roundingAdjustment));
        return new Result(cashPayable, roundingAdjustment);
    }

    public record Result(Money cashPayable, BigDecimal adjustment) {}
}
