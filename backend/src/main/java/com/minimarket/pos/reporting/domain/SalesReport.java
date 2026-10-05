package com.minimarket.pos.reporting.domain;

import com.minimarket.pos.sales.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SalesReport(
        LocalDate from,
        LocalDate to,
        PaymentMethod methodFilter,
        long recordedSales,
        BigDecimal recordedAmount,
        long voidedSales,
        BigDecimal voidedAmount,
        long netSales,
        BigDecimal netAmount,
        BigDecimal estimatedGrossProfit,
        List<PaymentSummary> payments,
        List<RefundSummary> refunds,
        List<TopProduct> topProducts) {

    public record PaymentSummary(
            PaymentMethod method,
            long recordedSales,
            BigDecimal recordedAmount,
            long voidedSales,
            BigDecimal voidedAmount,
            long netSales,
            BigDecimal netAmount) {}

    public record RefundSummary(
            PaymentMethod method,
            long cancellations,
            BigDecimal exactAmount,
            BigDecimal payableAmount) {}

    public record TopProduct(
            UUID productId,
            String code,
            String name,
            long quantity,
            BigDecimal revenue) {}
}
