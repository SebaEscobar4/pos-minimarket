package com.minimarket.pos.sales.application;

import com.minimarket.pos.sales.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SaleCommand(
        UUID idempotencyKey,
        List<Line> lines,
        PaymentMethod paymentMethod,
        BigDecimal cashReceived) {

    public record Line(UUID productId, Integer quantity) {}
}
