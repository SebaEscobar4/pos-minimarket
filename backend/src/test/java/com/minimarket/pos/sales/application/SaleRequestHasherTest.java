package com.minimarket.pos.sales.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minimarket.pos.sales.application.SaleRequestHasher.NormalizedLine;
import com.minimarket.pos.sales.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SaleRequestHasherTest {

    @Test
    void producesAStableHashAndChangesWithThePaymentIntent() {
        var lines = List.of(
                new NormalizedLine(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"), 2),
                new NormalizedLine(
                        UUID.fromString("00000000-0000-0000-0000-000000000002"), 1));

        String first = SaleRequestHasher.hash(lines, PaymentMethod.CASH, new BigDecimal("5000"));
        String repeated = SaleRequestHasher.hash(lines, PaymentMethod.CASH, new BigDecimal("5000"));
        String changed = SaleRequestHasher.hash(lines, PaymentMethod.CASH, new BigDecimal("6000"));

        assertThat(first).hasSize(64).isEqualTo(repeated).isNotEqualTo(changed);
    }
}
