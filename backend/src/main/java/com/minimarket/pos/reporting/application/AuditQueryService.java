package com.minimarket.pos.reporting.application;

import com.minimarket.pos.reporting.domain.AuditEvent;
import com.minimarket.pos.reporting.domain.AuditEventType;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Santiago");
    private static final int MAXIMUM_REFERENCE_LENGTH = 100;
    private static final int MAXIMUM_RANGE_DAYS = 366;

    private final AuditEventRepository repository;

    public AuditQueryService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AuditEventPage<AuditEvent> find(
            LocalDate from,
            LocalDate to,
            AuditEventType type,
            String reference,
            int page,
            int size) {
        validatePage(page, size);
        validateRange(from, to);
        String normalizedReference = normalizeReference(reference);
        return repository.find(
                from == null ? null : from.atStartOfDay(BUSINESS_ZONE).toInstant(),
                to == null ? null : to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant(),
                type,
                normalizedReference,
                page,
                size);
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw validation("La paginaci\u00f3n de auditor\u00eda no es v\u00e1lida.");
        }
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw validation("La fecha inicial no puede ser posterior a la fecha final.");
            }
            if (ChronoUnit.DAYS.between(from, to) >= MAXIMUM_RANGE_DAYS) {
                throw validation("El per\u00edodo de auditor\u00eda no puede superar 366 d\u00edas.");
            }
        }
    }

    private static String normalizeReference(String reference) {
        if (reference == null) {
            return null;
        }
        String normalized = reference.strip();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAXIMUM_REFERENCE_LENGTH) {
            throw validation("La referencia admite hasta 100 caracteres.");
        }
        return normalized;
    }

    private static ApplicationException validation(String message) {
        return new ApplicationException(ProblemType.VALIDATION, message);
    }
}
