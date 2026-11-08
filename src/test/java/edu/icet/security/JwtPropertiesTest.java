package edu.icet.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);
    private static final Duration SEVEN_DAYS = Duration.ofDays(7);

    @Test
    void missingSecretFailsWithInstructions() {
        assertThatThrownBy(() -> new JwtProperties("", "iss", FIFTEEN_MINUTES, SEVEN_DAYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is not set");
        assertThatThrownBy(() -> new JwtProperties(null, "iss", FIFTEEN_MINUTES, SEVEN_DAYS))
                .hasMessageContaining("JWT_SECRET is not set");
    }

    @Test
    void shortSecretFailsAndReportsItsLength() {
        assertThatThrownBy(() -> new JwtProperties("x".repeat(31), "iss", FIFTEEN_MINUTES, SEVEN_DAYS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short")
                .hasMessageContaining("31 bytes");
    }

    @Test
    void thirtyTwoByteSecretIsAccepted() {
        JwtProperties properties = new JwtProperties("x".repeat(32), "iss", FIFTEEN_MINUTES, SEVEN_DAYS);
        assertThat(properties.secretBytes()).hasSize(32);
    }

    @Test
    void nonPositiveLifetimeFails() {
        assertThatThrownBy(() -> new JwtProperties("x".repeat(32), "iss", Duration.ZERO, SEVEN_DAYS))
                .hasMessageContaining("must be positive");
    }

    @Test
    void toStringNeverContainsTheSecret() {
        String secret = "a-secret-value-that-must-not-leak-0123456789";
        assertThat(new JwtProperties(secret, "iss", FIFTEEN_MINUTES, SEVEN_DAYS).toString()).doesNotContain(secret);
    }
}
