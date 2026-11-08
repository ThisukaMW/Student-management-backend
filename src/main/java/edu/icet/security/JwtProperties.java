package edu.icet.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * JWT settings under {@code app.security.jwt}. The secret comes from the JWT_SECRET environment
 * variable (or .env); startup fails with a clear message when it is missing or too short.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("student-management") String issuer,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("7d") Duration refreshTokenTtl
) {

    // HS256 needs a key of at least 256 bits
    public static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not set. Add JWT_SECRET to .env or the environment: "
                    + "a random value of at least " + MIN_SECRET_BYTES + " bytes, e.g. from `openssl rand -base64 48`.");
        }
        int bytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET is too short: it is " + bytes + " bytes, but HS256 needs at least "
                    + MIN_SECRET_BYTES + ". Generate one with `openssl rand -base64 48`.");
        }
        if (accessTokenTtl.isNegative() || accessTokenTtl.isZero()
                || refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()) {
            throw new IllegalStateException("app.security.jwt token lifetimes must be positive");
        }
    }

    public byte[] secretBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    // Keep the secret out of logs and error messages
    @Override
    public String toString() {
        return "JwtProperties[issuer=" + issuer + ", accessTokenTtl=" + accessTokenTtl
                + ", refreshTokenTtl=" + refreshTokenTtl + "]";
    }
}
