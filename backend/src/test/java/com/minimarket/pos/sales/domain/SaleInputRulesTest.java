package com.minimarket.pos.sales.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SaleInputRulesTest {

    private static final Instant NOW = Instant.parse("2026-08-05T12:00:00Z");

    @Test
    void calculatesExactCashChangeAndElectronicPayments() {
        Payment cash = SaleInputRules.payment(
                PaymentMethod.CASH,
                new BigDecimal("5000"),
                Money.of(new BigDecimal("3496")),
                NOW);
        Payment card = SaleInputRules.payment(
                PaymentMethod.CARD, null, Money.of(new BigDecimal("3496")), NOW);

        assertThat(cash.amount().amount()).isEqualByComparingTo("3496");
        assertThat(cash.cashPayable().amount()).isEqualByComparingTo("3500");
        assertThat(cash.roundingAdjustment()).isEqualByComparingTo("4");
        assertThat(cash.cashReceived().amount()).isEqualByComparingTo("5000");
        assertThat(cash.change().amount()).isEqualByComparingTo("1500");
        assertThat(card.amount().amount()).isEqualByComparingTo("3496");
        assertThat(card.cashPayable()).isNull();
        assertThat(card.roundingAdjustment()).isNull();
        assertThat(card.cashReceived()).isNull();
        assertThat(card.change()).isNull();
    }

    @Test
    void rejectsInsufficientOrInvalidPaymentFields() {
        assertProblem(
                () -> SaleInputRules.payment(
                        PaymentMethod.CASH,
                        new BigDecimal("3499"),
                        Money.of(new BigDecimal("3496")),
                        NOW),
                ProblemType.CONFLICT);
        assertProblem(
                () -> SaleInputRules.payment(
                        PaymentMethod.CARD,
                        BigDecimal.ONE,
                        Money.of(BigDecimal.ONE),
                        NOW),
                ProblemType.VALIDATION);
        assertProblem(
                () -> SaleInputRules.payment(
                        PaymentMethod.CASH, null, Money.of(BigDecimal.ONE), NOW),
                ProblemType.VALIDATION);
    }

    @Test
    void validatesIntentQuantityAndPaginationBoundaries() {
        SaleInputRules.validateIntent(
                UUID.randomUUID(), List.of("line"), PaymentMethod.TRANSFER);
        assertThat(SaleInputRules.quantity(1_000_000)).isEqualTo(1_000_000);
        SaleInputRules.validatePage(0, 100);

        assertProblem(
                () -> SaleInputRules.validateIntent(null, List.of("line"), PaymentMethod.CASH),
                ProblemType.VALIDATION);
        assertProblem(
                () -> SaleInputRules.validateIntent(UUID.randomUUID(), List.of(), PaymentMethod.CASH),
                ProblemType.VALIDATION);
        assertProblem(() -> SaleInputRules.quantity(0), ProblemType.VALIDATION);
        assertProblem(() -> SaleInputRules.validatePage(0, 101), ProblemType.VALIDATION);
    }

    private void assertProblem(Runnable action, ProblemType type) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).problemType())
                .isEqualTo(type);
    }
}
