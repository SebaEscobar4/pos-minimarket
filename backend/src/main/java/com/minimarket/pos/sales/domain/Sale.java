package com.minimarket.pos.sales.domain;

import com.minimarket.pos.shared.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Sale(
        UUID id,
        long folio,
        UUID cashSessionId,
        UUID actorId,
        String actorDisplayName,
        Instant confirmedAt,
        SaleStatus status,
        Money total,
        UUID idempotencyKey,
        String requestHash,
        List<SaleLine> lines,
        Payment payment,
        SaleCancellation cancellation) {

    public String displayFolio() {
        return "V-%06d".formatted(folio);
    }
}
