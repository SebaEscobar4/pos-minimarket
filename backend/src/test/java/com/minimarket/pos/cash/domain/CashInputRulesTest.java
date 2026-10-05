package com.minimarket.pos.cash.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CashInputRulesTest {

    @Test
    void acceptsZeroWholePesosAndTheSafeMaximum() {
        assertThat(CashInputRules.openingAmount(BigDecimal.ZERO).amount())
                .isEqualByComparingTo("0");
        assertThat(CashInputRules.openingAmount(new BigDecimal("25000.00")).amount())
                .isEqualByComparingTo("25000");
        assertThat(CashInputRules.openingAmount(new BigDecimal("9007199254740991")).amount())
                .isEqualByComparingTo("9007199254740991");
    }

    @Test
    void rejectsMissingNegativeFractionalAndUnsafeAmounts() {
        assertInvalid(null);
        assertInvalid(new BigDecimal("-1"));
        assertInvalid(new BigDecimal("0.01"));
        assertInvalid(new BigDecimal("9007199254740992"));
    }

    @Test
    void validatesMovementReasonAmountAndPagination() {
        assertThat(CashInputRules.movementAmount(BigDecimal.ONE).amount())
                .isEqualByComparingTo("1");
        assertThat(CashInputRules.countedCash(BigDecimal.ZERO).amount())
                .isEqualByComparingTo("0");
        assertThat(CashInputRules.requiredReason("  Compra de bolsas  "))
                .isEqualTo("Compra de bolsas");
        CashInputRules.validatePage(0, 100);

        assertProblem(() -> CashInputRules.movementAmount(BigDecimal.ZERO));
        assertProblem(() -> CashInputRules.requiredReason(" "));
        assertProblem(() -> CashInputRules.requiredReason("x".repeat(501)));
        assertProblem(() -> CashInputRules.validatePage(-1, 20));
        assertProblem(() -> CashInputRules.validatePage(0, 101));
    }

    private void assertInvalid(BigDecimal amount) {
        assertProblem(() -> CashInputRules.openingAmount(amount));
    }

    private void assertProblem(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).problemType())
                .isEqualTo(ProblemType.VALIDATION);
    }
}
