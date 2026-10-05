package com.minimarket.pos.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount) {

    public static final BigDecimal MAXIMUM_AMOUNT = new BigDecimal("9007199254740991");

    public Money {
        Objects.requireNonNull(amount, "amount is required");
        if (amount.signum() < 0
                || amount.compareTo(MAXIMUM_AMOUNT) > 0
                || amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("money must be a non-negative safe CLP integer");
        }
        amount = amount.setScale(0, RoundingMode.UNNECESSARY);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }
}
