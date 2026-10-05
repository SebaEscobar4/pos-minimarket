package com.minimarket.pos.reporting.domain;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        String id,
        AuditEventType type,
        Instant occurredAt,
        UUID actorId,
        String actorDisplayName,
        String reference,
        String summary) {}
