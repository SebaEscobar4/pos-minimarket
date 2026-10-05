package com.minimarket.pos.catalog.application;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCommand(
        String code,
        String name,
        UUID categoryId,
        BigDecimal purchasePrice,
        BigDecimal salePrice,
        Integer minimumStock) {}
