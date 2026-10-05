package com.minimarket.pos.reporting.application;

import com.minimarket.pos.inventory.domain.InventoryMovementType;
import com.minimarket.pos.reporting.domain.CashReport;
import com.minimarket.pos.reporting.domain.InventoryReport;
import com.minimarket.pos.reporting.domain.SalesReport;
import com.minimarket.pos.sales.domain.PaymentMethod;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface OperationalReportRepository {

    SalesReport sales(
            LocalDate from,
            LocalDate to,
            Instant fromInstant,
            Instant toExclusive,
            PaymentMethod method);

    InventoryReport inventory(
            LocalDate from,
            LocalDate to,
            Instant fromInstant,
            Instant toExclusive,
            InventoryMovementType type,
            UUID productId,
            int page,
            int size);

    CashReport cash(
            LocalDate from,
            LocalDate to,
            Instant fromInstant,
            Instant toExclusive,
            int page,
            int size);
}
