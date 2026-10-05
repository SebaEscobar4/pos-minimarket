package com.minimarket.pos.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AuthenticationAttemptLimiterTest {

    @Test
    void blocksByAccountAndOriginAndClearsTheAccountAfterSuccess() {
        MutableClock clock = new MutableClock();
        AuthenticationAttemptLimiter limiter = new AuthenticationAttemptLimiter(clock, properties());

        assertThat(limiter.isBlocked(null, null)).isFalse();
        limiter.recordFailure(" Admin ", "10.0.0.1");
        limiter.recordFailure("ADMIN", "10.0.0.1");
        assertThat(limiter.isBlocked("admin", "10.0.0.2")).isTrue();
        assertThat(limiter.isBlocked("another", "10.0.0.1")).isTrue();

        limiter.recordSuccess("ADMIN");
        assertThat(limiter.isBlocked("admin", "10.0.0.2")).isFalse();
        assertThat(limiter.isBlocked("another", "10.0.0.1")).isTrue();
    }

    @Test
    void expiresTheFixedFailureWindow() {
        MutableClock clock = new MutableClock();
        AuthenticationAttemptLimiter limiter = new AuthenticationAttemptLimiter(clock, properties());
        limiter.recordFailure("admin", "10.0.0.1");
        limiter.recordFailure("admin", "10.0.0.1");

        clock.advance(Duration.ofMinutes(15));
        assertThat(limiter.isBlocked("admin", "10.0.0.1")).isFalse();

        limiter.recordFailure("admin", "10.0.0.1");
        assertThat(limiter.isBlocked("admin", "10.0.0.1")).isFalse();
    }

    private IdentityProperties properties() {
        return new IdentityProperties(
                new IdentityProperties.Bootstrap("", "", "Administrador"),
                new IdentityProperties.Password(12, 128),
                new IdentityProperties.Authentication(2, Duration.ofMinutes(15)));
    }

    private static final class MutableClock extends Clock {
        private Instant current = Instant.parse("2026-08-04T00:00:00Z");

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
