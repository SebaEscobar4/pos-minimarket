package com.minimarket.pos.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minimarket.pos.identity.domain.UserAccount;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class BootstrapAdminInitializerTest {

    @Test
    void doesNothingWhenAnyUserAlreadyExists() {
        InMemoryRepository repository = new InMemoryRepository();
        repository.count = 1;
        initializer(repository, "admin", "Temporal-admin-2026").run(null);
        assertThat(repository.created).isNull();
    }

    @Test
    void waitsSafelyWhenBothBootstrapSecretsAreMissing() {
        InMemoryRepository repository = new InMemoryRepository();
        initializer(repository, "", "").run(null);
        assertThat(repository.created).isNull();
    }

    @Test
    void rejectsAnIncompleteBootstrapConfiguration() {
        InMemoryRepository repository = new InMemoryRepository();
        assertThatThrownBy(() -> initializer(repository, "admin", "").run(null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> initializer(repository, "", "Temporal-admin-2026").run(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createsTheFirstActiveAdministratorWithAForcedPasswordChange() {
        InMemoryRepository repository = new InMemoryRepository();
        initializer(repository, " ADMIN ", "Temporal-admin-2026").run(null);

        assertThat(repository.created.username()).isEqualTo("admin");
        assertThat(repository.created.passwordHash()).isEqualTo("encoded:Temporal-admin-2026");
        assertThat(repository.created.role().name()).isEqualTo("ADMIN");
        assertThat(repository.created.status().name()).isEqualTo("ACTIVE");
        assertThat(repository.created.passwordChangeRequired()).isTrue();
    }

    private BootstrapAdminInitializer initializer(
            InMemoryRepository repository, String username, String password) {
        IdentityProperties properties = new IdentityProperties(
                new IdentityProperties.Bootstrap(username, password, "Administrador"),
                new IdentityProperties.Password(12, 128),
                new IdentityProperties.Authentication(5, Duration.ofMinutes(15)));
        return new BootstrapAdminInitializer(repository, new PrefixPasswordEncoder(), properties);
    }

    private static final class InMemoryRepository implements UserAccountRepository {
        private long count;
        private UserAccount created;

        @Override
        public long countUsers() {
            return count;
        }

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return Optional.ofNullable(created).filter(account -> account.username().equals(username));
        }

        @Override
        public void create(UserAccount account) {
            created = account;
            count++;
        }

        @Override
        public void updatePassword(
                String username, String passwordHash, boolean passwordChangeRequired) {}
    }

    private static final class PrefixPasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encodedPassword.equals(encode(rawPassword));
        }
    }
}
