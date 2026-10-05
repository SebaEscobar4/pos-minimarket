package com.minimarket.pos.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdentityInputRulesTest {

    @Test
    void normalizesAValidUsername() {
        assertThat(IdentityInputRules.normalizeUsername("  ADMIN_01  ")).isEqualTo("admin_01");
    }

    @Test
    void rejectsMissingOrInvalidUsernames() {
        assertThatThrownBy(() -> IdentityInputRules.normalizeUsername(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdentityInputRules.normalizeUsername("ab"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdentityInputRules.normalizeUsername("usuario con espacios"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesAndTrimsDisplayNames() {
        assertThat(IdentityInputRules.requireDisplayName("  Ana  ")).isEqualTo("Ana");
        assertThatThrownBy(() -> IdentityInputRules.requireDisplayName(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdentityInputRules.requireDisplayName("x".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesPasswordBoundariesWithoutAlteringTheSecret() {
        assertThat(IdentityInputRules.requirePassword("123456789012", 12, 128))
                .isEqualTo("123456789012");
        assertThatThrownBy(() -> IdentityInputRules.requirePassword(null, 12, 128))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdentityInputRules.requirePassword("corta", 12, 128))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdentityInputRules.requirePassword("x".repeat(129), 12, 128))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
