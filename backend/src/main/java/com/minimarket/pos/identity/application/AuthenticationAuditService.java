package com.minimarket.pos.identity.application;

import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationAuditService {

    public enum EventType {
        LOGIN_SUCCEEDED,
        LOGIN_FAILED,
        LOGIN_RATE_LIMITED,
        PASSWORD_CHANGED,
        LOGOUT_SUCCEEDED
    }

    private final JdbcTemplate jdbcTemplate;

    public AuthenticationAuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(EventType type, String usernameCandidate, String sourceAddress) {
        record(type, null, usernameCandidate, sourceAddress);
    }

    public void record(EventType type, IdentityPrincipal principal, String sourceAddress) {
        record(type, principal, principal.getUsername(), sourceAddress);
    }

    private void record(
            EventType type,
            IdentityPrincipal principal,
            String usernameCandidate,
            String sourceAddress) {
        String username = safeUsername(usernameCandidate);
        String source = sourceAddress == null || sourceAddress.isBlank() ? "unknown" : sourceAddress;
        if (source.length() > 64) {
            source = source.substring(0, 64);
        }
        jdbcTemplate.update(
                """
                INSERT INTO authentication_audit_event (
                    user_id, username, event_type, source_address
                ) VALUES (?, ?, ?, ?)
                """,
                principal == null ? null : principal.id(),
                username,
                type.name(),
                source);
    }

    private String safeUsername(String candidate) {
        if (candidate == null) {
            return "<missing>";
        }
        String value = candidate.strip().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return "<missing>";
        }
        return value.length() <= 50 ? value : value.substring(0, 50);
    }
}
