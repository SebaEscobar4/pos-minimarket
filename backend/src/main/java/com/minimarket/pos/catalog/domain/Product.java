package com.minimarket.pos.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Product(
        UUID id,
        String code,
        String name,
        UUID categoryId,
        String categoryName,
        BigDecimal purchasePrice,
        BigDecimal salePrice,
        int minimumStock,
        ProductStatus status,
        int currentStock,
        Instant createdAt,
        Instant updatedAt) {

    public boolean available() {
        return status == ProductStatus.ACTIVE && currentStock > 0;
    }
}
