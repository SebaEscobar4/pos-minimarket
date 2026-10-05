package com.minimarket.pos.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationAttemptLimiter {

    private record AttemptWindow(int failures, Instant expiresAt) {}

    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final IdentityProperties properties;

    public AuthenticationAttemptLimiter(Clock clock, IdentityProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public boolean isBlocked(String username, String sourceAddress) {
        Instant now = clock.instant();
        return blocked(accountKey(username), now) || blocked(originKey(sourceAddress), now);
    }

    public void recordFailure(String username, String sourceAddress) {
        Instant now = clock.instant();
        increment(accountKey(username), now);
        increment(originKey(sourceAddress), now);
    }

    public void recordSuccess(String username) {
        attempts.remove(accountKey(username));
    }

    private boolean blocked(String key, Instant now) {
        AttemptWindow window = attempts.get(key);
        if (window == null) {
            return false;
        }
        if (!now.isBefore(window.expiresAt())) {
            attempts.remove(key, window);
            return false;
        }
        return window.failures() >= properties.authentication().maximumFailures();
    }

    private void increment(String key, Instant now) {
        attempts.compute(key, (ignored, current) -> {
            if (current == null || !now.isBefore(current.expiresAt())) {
                return new AttemptWindow(1, now.plus(properties.authentication().failureWindow()));
            }
            return new AttemptWindow(current.failures() + 1, current.expiresAt());
        });
    }

    private String accountKey(String username) {
        String value = username == null ? "<missing>" : username.strip().toLowerCase(Locale.ROOT);
        return "account:" + value;
    }

    private String originKey(String sourceAddress) {
        return "origin:" + (sourceAddress == null ? "unknown" : sourceAddress);
    }
}
