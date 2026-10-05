package com.minimarket.pos.reporting.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.minimarket.pos.inventory.domain.InventoryMovementType;
import com.minimarket.pos.sales.domain.PaymentMethod;
import com.minimarket.pos.shared.application.error.ApplicationException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperationalReportServiceTest {

    @Mock
    private OperationalReportRepository repository;

    private OperationalReportService service;

    @BeforeEach
    void setUp() {
        service = new OperationalReportService(
                repository, Clock.fixed(Instant.parse("2026-08-05T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void usesLastThirtyBusinessDaysByDefault() {
        service.sales(null, null, PaymentMethod.CASH);

        verify(repository)
                .sales(
                        LocalDate.of(2026, 7, 7),
                        LocalDate.of(2026, 8, 5),
                        Instant.parse("2026-07-07T04:00:00Z"),
                        Instant.parse("2026-08-06T04:00:00Z"),
                        PaymentMethod.CASH);
    }

    @Test
    void appliesInventoryFiltersAndChileanInclusiveDates() {
        UUID productId = UUID.randomUUID();

        service.inventory(
                LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 5),
                InventoryMovementType.SALE_OUT,
                productId,
                2,
                50);

        verify(repository)
                .inventory(
                        LocalDate.of(2026, 8, 4),
                        LocalDate.of(2026, 8, 5),
                        Instant.parse("2026-08-04T04:00:00Z"),
                        Instant.parse("2026-08-06T04:00:00Z"),
                        InventoryMovementType.SALE_OUT,
                        productId,
                        2,
                        50);
    }

    @Test
    void rejectsInvalidPeriodsAndPagination() {
        assertThatThrownBy(() -> service.sales(
                        LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 5), null))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> service.cash(
                        LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2), 0, 20))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> service.inventory(null, null, null, null, -1, 20))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> service.cash(null, null, 0, 101))
                .isInstanceOf(ApplicationException.class);
    }
}
