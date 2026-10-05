package com.minimarket.pos.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minimarket.pos.identity.application.AuthenticationAuditService.EventType;
import com.minimarket.pos.identity.domain.UserAccount;
import com.minimarket.pos.identity.domain.UserRole;
import com.minimarket.pos.identity.domain.UserStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AuthenticationAuditServiceTest {

    @Test
    void sanitizesUntrustedAuditFieldsAndAssociatesKnownUsers() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        AuthenticationAuditService service = new AuthenticationAuditService(jdbcTemplate);
        service.record(EventType.LOGIN_FAILED, (String) null, null);
        service.record(EventType.LOGIN_FAILED, "   ", "");
        service.record(EventType.LOGIN_FAILED, "A".repeat(60), "x".repeat(70));

        UUID userId = UUID.randomUUID();
        IdentityPrincipal principal = new IdentityPrincipal(new UserAccount(
                userId, "admin", "Administrador", "hash", UserRole.ADMIN, UserStatus.ACTIVE, false));
        service.record(EventType.LOGIN_SUCCEEDED, principal, "127.0.0.1");

        assertThat(jdbcTemplate.calls.get(0)).containsExactly(null, "<missing>", "LOGIN_FAILED", "unknown");
        assertThat(jdbcTemplate.calls.get(1)).containsExactly(null, "<missing>", "LOGIN_FAILED", "unknown");
        assertThat(jdbcTemplate.calls.get(2).get(1)).isEqualTo("a".repeat(50));
        assertThat(jdbcTemplate.calls.get(2).get(3)).isEqualTo("x".repeat(64));
        assertThat(jdbcTemplate.calls.get(3))
                .containsExactly(userId, "admin", "LOGIN_SUCCEEDED", "127.0.0.1");
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private final List<List<Object>> calls = new ArrayList<>();

        @Override
        public int update(String sql, Object... args) {
            calls.add(Arrays.asList(args));
            return 1;
        }
    }
}
