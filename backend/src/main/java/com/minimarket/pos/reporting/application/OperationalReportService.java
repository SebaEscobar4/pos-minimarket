package com.minimarket.pos.reporting.application;

import com.minimarket.pos.inventory.domain.InventoryMovementType;
import com.minimarket.pos.reporting.domain.CashReport;
import com.minimarket.pos.reporting.domain.InventoryReport;
import com.minimarket.pos.reporting.domain.SalesReport;
import com.minimarket.pos.sales.domain.PaymentMethod;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationalReportService {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Santiago");
    private static final int MAXIMUM_RANGE_DAYS = 366;
    private final OperationalReportRepository repository;
    private final Clock clock;

    public OperationalReportService(OperationalReportRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public SalesReport sales(LocalDate from, LocalDate to, PaymentMethod method) {
        Period period = period(from, to);
        return repository.sales(
                period.from(), period.to(), period.fromInstant(), period.toExclusive(), method);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public InventoryReport inventory(
            LocalDate from,
            LocalDate to,
            InventoryMovementType type,
            UUID productId,
            int page,
            int size) {
        validatePage(page, size);
        Period period = period(from, to);
        return repository.inventory(
                period.from(),
                period.to(),
                period.fromInstant(),
                period.toExclusive(),
                type,
                productId,
                page,
                size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public CashReport cash(LocalDate from, LocalDate to, int page, int size) {
        validatePage(page, size);
        Period period = period(from, to);
        return repository.cash(
                period.from(),
                period.to(),
                period.fromInstant(),
                period.toExclusive(),
                page,
                size);
    }

    private Period period(LocalDate requestedFrom, LocalDate requestedTo) {
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        LocalDate to = requestedTo == null ? today : requestedTo;
        LocalDate from = requestedFrom == null ? to.minusDays(29) : requestedFrom;
        if (from.isAfter(to)) {
            throw validation("La fecha inicial no puede ser posterior a la fecha final.");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAXIMUM_RANGE_DAYS) {
            throw validation("El per\u00edodo del reporte no puede superar 366 d\u00edas.");
        }
        return new Period(
                from,
                to,
                from.atStartOfDay(BUSINESS_ZONE).toInstant(),
                to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant());
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw validation("La paginaci\u00f3n del reporte no es v\u00e1lida.");
        }
    }

    private static ApplicationException validation(String message) {
        return new ApplicationException(ProblemType.VALIDATION, message);
    }

    private record Period(LocalDate from, LocalDate to, Instant fromInstant, Instant toExclusive) {}
}
