package com.minimarket.pos.reporting.application;

import com.minimarket.pos.reporting.domain.AuditEvent;
import com.minimarket.pos.reporting.domain.AuditEventType;
import java.time.Instant;

public interface AuditEventRepository {

    AuditEventPage<AuditEvent> find(
            Instant from,
            Instant toExclusive,
            AuditEventType type,
            String reference,
            int page,
            int size);
}
