package com.minimarket.pos.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void keepsClpAsAnExactScaleZeroValue() {
        Money money = Money.of(new BigDecimal("25000.00"));

        assertThat(money.amount()).isEqualByComparingTo("25000");
        assertThat(money.amount().scale()).isZero();
    }

    @Test
    void rejectsNegativeFractionalNullAndUnsafeJsonAmounts() {
        assertThatThrownBy(() -> Money.of(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of(new BigDecimal("1.5")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of(new BigDecimal("9007199254740992")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
