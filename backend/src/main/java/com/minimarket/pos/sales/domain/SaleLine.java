package com.minimarket.pos.sales.domain;

import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
import java.util.UUID;

public record SaleLine(
        UUID id,
        UUID productId,
        String productName,
        String productCode,
        int quantity,
        Money unitSalePrice,
        BigDecimal unitEstimatedCost,
        Money subtotal) {}
