package com.minimarket.pos.sales.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CashRoundingTest {

    @Test
    void appliesTheChileanRuleToEveryPossibleLastDigit() {
        assertRounding("100", "100", "0");
        assertRounding("101", "100", "-1");
        assertRounding("102", "100", "-2");
        assertRounding("103", "100", "-3");
        assertRounding("104", "100", "-4");
        assertRounding("105", "100", "-5");
        assertRounding("106", "110", "4");
        assertRounding("107", "110", "3");
        assertRounding("108", "110", "2");
        assertRounding("109", "110", "1");
    }

    private void assertRounding(String exact, String payable, String adjustment) {
        CashRounding.Result result = CashRounding.apply(Money.of(new BigDecimal(exact)));

        assertThat(result.cashPayable().amount()).isEqualByComparingTo(payable);
        assertThat(result.adjustment()).isEqualByComparingTo(adjustment);
    }
}
