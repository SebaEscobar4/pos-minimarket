package com.minimarket.pos.identity.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("pos.identity")
public record IdentityProperties(
        Bootstrap bootstrap, Password password, Authentication authentication) {

    public record Bootstrap(String username, String password, String displayName) {}

    public record Password(int minimumLength, int maximumLength) {}

    public record Authentication(int maximumFailures, Duration failureWindow) {}
}
