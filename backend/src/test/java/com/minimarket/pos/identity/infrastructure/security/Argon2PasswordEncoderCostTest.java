package com.minimarket.pos.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class Argon2PasswordEncoderCostTest {

    @Test
    void measuresTheConfiguredArgon2idCostAndVerifiesTheResult() {
        PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 19_456, 2);
        encoder.encode("warmup-password-not-a-secret");

        long started = System.nanoTime();
        String hash = encoder.encode("synthetic-password-not-a-secret");
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();

        assertThat(hash).startsWith("$argon2id$");
        assertThat(encoder.matches("synthetic-password-not-a-secret", hash)).isTrue();
        assertThat(encoder.matches("incorrect", hash)).isFalse();
        System.out.printf("Argon2id benchmark: memory=19456 KiB iterations=2 parallelism=1 elapsed=%d ms%n", elapsedMillis);
    }
}
