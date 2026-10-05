package com.minimarket.pos.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minimarket.pos.identity.domain.UserAccount;
import com.minimarket.pos.identity.domain.UserRole;
import com.minimarket.pos.identity.domain.UserStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentityPrincipalTest {

    @Test
    void exposesOnlyTheSecurityDataNeededBySpringSecurity() {
        UUID id = UUID.randomUUID();
        IdentityPrincipal principal = new IdentityPrincipal(new UserAccount(
                id, "admin", "Administración", "hash", UserRole.ADMIN, UserStatus.ACTIVE, true));

        assertThat(principal.id()).isEqualTo(id);
        assertThat(principal.getUsername()).isEqualTo("admin");
        assertThat(principal.displayName()).isEqualTo("Administración");
        assertThat(principal.getPassword()).isEqualTo("hash");
        assertThat(principal.role()).isEqualTo("ADMIN");
        assertThat(principal.passwordChangeRequired()).isTrue();
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void disablesAnInactiveUser() {
        IdentityPrincipal principal = new IdentityPrincipal(new UserAccount(
                UUID.randomUUID(),
                "seller",
                "Vendedor",
                "hash",
                UserRole.SELLER,
                UserStatus.INACTIVE,
                false));

        assertThat(principal.isEnabled()).isFalse();
    }
}
