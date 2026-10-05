package com.minimarket.pos.sales.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SaleCancellationRulesTest {

    private static final UUID SALE_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-05T20:00:00Z");

    @Test
    void createsCashAndElectronicRefundSnapshots() {
        SaleCancellation cash = SaleCancellationRules.create(
                SALE_ID,
                PaymentMethod.CASH,
                Money.of(new BigDecimal("406")),
                "  Error de cobro  ",
                ACTOR_ID,
                " Administración ",
                NOW);
        SaleCancellation card = SaleCancellationRules.create(
                SALE_ID,
                PaymentMethod.CARD,
                Money.of(new BigDecimal("406")),
                "Error de cobro",
                ACTOR_ID,
                "Administración",
                NOW);

        assertThat(cash.cashPayable().amount()).isEqualByComparingTo("410");
        assertThat(cash.roundingAdjustment()).isEqualByComparingTo("4");
        assertThat(cash.reason()).isEqualTo("Error de cobro");
        assertThat(card.cashPayable()).isNull();
        assertThat(card.roundingAdjustment()).isNull();
    }

    @Test
    void rejectsMissingOrOversizedReasons() {
        assertThatThrownBy(() -> SaleCancellationRules.create(
                        SALE_ID,
                        PaymentMethod.CASH,
                        Money.of(BigDecimal.TEN),
                        " ",
                        ACTOR_ID,
                        "Administración",
                        NOW))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> SaleCancellationRules.create(
                        SALE_ID,
                        PaymentMethod.CASH,
                        Money.of(BigDecimal.TEN),
                        "x".repeat(501),
                        ACTOR_ID,
                        "Administración",
                        NOW))
                .isInstanceOf(ApplicationException.class);
    }
}
