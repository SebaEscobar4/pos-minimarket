package com.minimarket.pos.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minimarket.pos.identity.domain.UserAccount;
import com.minimarket.pos.identity.domain.UserRole;
import com.minimarket.pos.identity.domain.UserStatus;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordChangeServiceTest {

    private final RecordingRepository repository = new RecordingRepository();
    private final RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
    private final PasswordEncoder encoder = new PrefixPasswordEncoder();
    private final PasswordChangeService service = new PasswordChangeService(
            repository,
            encoder,
            new IdentityProperties(
                    new IdentityProperties.Bootstrap("", "", "Administrador"),
                    new IdentityProperties.Password(12, 128),
                    new IdentityProperties.Authentication(5, Duration.ofMinutes(15))),
            jdbcTemplate);
    private final IdentityPrincipal principal = new IdentityPrincipal(new UserAccount(
            UUID.randomUUID(),
            "admin",
            "Administrador",
            "encoded:actual-segura",
            UserRole.ADMIN,
            UserStatus.ACTIVE,
            true));

    @Test
    void rejectsAnIncorrectCurrentPassword() {
        assertProblem(
                () -> service.changePassword(principal, "incorrecta", "nueva-segura-2026"),
                ProblemType.AUTHENTICATION);
    }

    @Test
    void rejectsAnInvalidOrRepeatedNewPassword() {
        assertProblem(
                () -> service.changePassword(principal, "actual-segura", "corta"),
                ProblemType.VALIDATION);
        assertProblem(
                () -> service.changePassword(principal, "actual-segura", "actual-segura"),
                ProblemType.VALIDATION);
    }

    @Test
    void updatesTheHashAndRevokesEverySessionForTheUser() {
        service.changePassword(principal, "actual-segura", "nueva-segura-2026");

        assertThat(repository.updatedUsername).isEqualTo("admin");
        assertThat(repository.updatedHash).isEqualTo("encoded:nueva-segura-2026");
        assertThat(repository.passwordChangeRequired).isFalse();
        assertThat(jdbcTemplate.arguments).containsExactly("admin");
    }

    private void assertProblem(ThrowingCall call, ProblemType expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(ApplicationException.class)
                .satisfies(exception -> assertThat(((ApplicationException) exception).problemType())
                        .isEqualTo(expected));
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }

    private static final class RecordingRepository implements UserAccountRepository {
        private String updatedUsername;
        private String updatedHash;
        private boolean passwordChangeRequired;

        @Override
        public long countUsers() {
            return 0;
        }

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return Optional.empty();
        }

        @Override
        public void create(UserAccount account) {}

        @Override
        public void updatePassword(
                String username, String passwordHash, boolean passwordChangeRequired) {
            this.updatedUsername = username;
            this.updatedHash = passwordHash;
            this.passwordChangeRequired = passwordChangeRequired;
        }
    }

    private static final class PrefixPasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return rawPassword != null && encodedPassword.equals(encode(rawPassword));
        }
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private Object[] arguments;

        @Override
        public int update(String sql, Object... args) {
            arguments = args;
            return 1;
        }
    }
}
