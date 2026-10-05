package com.minimarket.pos.reporting.domain;

import com.minimarket.pos.inventory.domain.InventoryMovementType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InventoryReport(
        LocalDate from,
        LocalDate to,
        InventoryMovementType typeFilter,
        UUID productFilter,
        List<LowStockProduct> lowStock,
        List<MovementSummary> totals,
        ReportPage<Movement> movements) {

    public record LowStockProduct(
            UUID productId,
            String code,
            String name,
            int quantity,
            int minimumStock,
            int shortage) {}

    public record MovementSummary(
            InventoryMovementType type,
            long movements,
            long quantity,
            long netDelta) {}

    public record Movement(
            UUID id,
            UUID productId,
            String productCode,
            String productName,
            InventoryMovementType type,
            int quantity,
            int delta,
            int previousBalance,
            int resultingBalance,
            String reason,
            String reference,
            String actorDisplayName,
            Instant occurredAt) {}
}
