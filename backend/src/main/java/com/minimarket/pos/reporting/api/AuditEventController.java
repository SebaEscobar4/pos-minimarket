package com.minimarket.pos.reporting.api;

import com.minimarket.pos.reporting.application.AuditEventPage;
import com.minimarket.pos.reporting.application.AuditQueryService;
import com.minimarket.pos.reporting.domain.AuditEvent;
import com.minimarket.pos.reporting.domain.AuditEventType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-events")
public class AuditEventController {

    private final AuditQueryService service;

    public AuditEventController(AuditQueryService service) {
        this.service = service;
    }

    @GetMapping
    public AuditEventPageResponse find(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) AuditEventType type,
            @RequestParam(required = false) String reference,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return AuditEventPageResponse.from(service.find(from, to, type, reference, page, size));
    }

    public record AuditEventPageResponse(
            List<AuditEventResponse> items,
            int page,
            int size,
            long totalElements,
            long totalPages) {

        static AuditEventPageResponse from(AuditEventPage<AuditEvent> events) {
            return new AuditEventPageResponse(
                    events.items().stream().map(AuditEventResponse::from).toList(),
                    events.page(),
                    events.size(),
                    events.totalElements(),
                    events.totalPages());
        }
    }

    public record AuditEventResponse(
            String id,
            AuditEventType type,
            Instant occurredAt,
            UUID actorId,
            String actorDisplayName,
            String reference,
            String summary) {

        static AuditEventResponse from(AuditEvent event) {
            return new AuditEventResponse(
                    event.id(),
                    event.type(),
                    event.occurredAt(),
                    event.actorId(),
                    event.actorDisplayName(),
                    event.reference(),
                    event.summary());
        }
    }
}
