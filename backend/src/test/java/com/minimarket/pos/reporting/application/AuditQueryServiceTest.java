package com.minimarket.pos.reporting.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minimarket.pos.reporting.domain.AuditEvent;
import com.minimarket.pos.reporting.domain.AuditEventType;
import com.minimarket.pos.shared.application.error.ApplicationException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {

    @Mock
    private AuditEventRepository repository;

    @Test
    void convertsInclusiveChileanDatesAndNormalizesReference() {
        AuditQueryService service = new AuditQueryService(repository);
        when(repository.find(
                        Instant.parse("2026-08-05T04:00:00Z"),
                        Instant.parse("2026-08-07T04:00:00Z"),
                        AuditEventType.SALE_VOIDED,
                        "V-000001",
                        0,
                        20))
                .thenReturn(new AuditEventPage<AuditEvent>(List.of(), 0, 20, 0));

        service.find(
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 6),
                AuditEventType.SALE_VOIDED,
                "  V-000001  ",
                0,
                20);

        verify(repository)
                .find(
                        Instant.parse("2026-08-05T04:00:00Z"),
                        Instant.parse("2026-08-07T04:00:00Z"),
                        AuditEventType.SALE_VOIDED,
                        "V-000001",
                        0,
                        20);
    }

    @Test
    void rejectsInvalidRangesPaginationAndOversizedReferences() {
        AuditQueryService service = new AuditQueryService(repository);

        assertThatThrownBy(() -> service.find(
                        LocalDate.of(2026, 8, 6),
                        LocalDate.of(2026, 8, 5),
                        null,
                        null,
                        0,
                        20))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> service.find(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2026, 1, 2),
                        null,
                        null,
                        0,
                        20))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> service.find(null, null, null, null, -1, 20))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> service.find(null, null, null, "x".repeat(101), 0, 20))
                .isInstanceOf(ApplicationException.class);
    }
}
