package com.minimarket.pos.reporting.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CashReport(
        LocalDate from,
        LocalDate to,
        long openedSessions,
        long closedSessions,
        BigDecimal openingAmount,
        BigDecimal expectedCash,
        BigDecimal countedCash,
        BigDecimal difference,
        ReportPage<Session> sessions) {

    public record Session(
            UUID id,
            String status,
            String openedBy,
            Instant openedAt,
            BigDecimal openingAmount,
            String closedBy,
            Instant closedAt,
            BigDecimal expectedCash,
            BigDecimal countedCash,
            BigDecimal difference) {}
}
