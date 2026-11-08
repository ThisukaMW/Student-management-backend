package edu.icet.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import edu.icet.dto.response.AuthResponse;
import edu.icet.entity.RefreshToken;
import edu.icet.entity.Role;
import edu.icet.entity.User;
import edu.icet.exception.InvalidTokenException;
import edu.icet.repository.RefreshTokenRepository;
import edu.icet.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static edu.icet.service.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");
    private static final JwtProperties PROPERTIES = new JwtProperties(
            "unit-test-secret-that-is-at-least-32-bytes", "student-management",
            Duration.ofMinutes(15), Duration.ofDays(7));
    private static final SecretKey KEY = new SecretKeySpec(PROPERTIES.secretBytes(), "HmacSHA256");

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private TokenServiceImpl tokenService;
    private User nimal;

    @BeforeEach
    void setUp() {
        tokenService = new TokenServiceImpl(new NimbusJwtEncoder(new ImmutableSecret<>(KEY)), PROPERTIES,
                refreshTokenRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        nimal = user(10L, "nimal@example.com", Role.STUDENT);
    }

    private static Jwt decode(String token) {
        // Validate the signature only; the fixed clock is in the past relative to the real one
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(jwt -> org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success());
        return decoder.decode(token);
    }

    private RefreshToken stored(String raw, User owner) {
        return RefreshToken.builder()
                .id(5L)
                .user(owner)
                .tokenHash(TokenServiceImpl.hash(raw))
                .createdAt(NOW.minus(Duration.ofDays(1)))
                .expiresAt(NOW.plus(Duration.ofDays(6)))
                .build();
    }

    @Test
    void issueTokensSignsAccessTokenWithUserClaims() {
        AuthResponse response = tokenService.issueTokens(nimal);

        Jwt jwt = decode(response.accessToken());
        assertThat(jwt.getSubject()).isEqualTo("10");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("nimal@example.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("STUDENT");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("student-management");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.user().id()).isEqualTo(10L);
        assertThat(response.user().role()).isEqualTo(Role.STUDENT);
    }

    @Test
    void issueTokensStoresOnlyTheHashOfTheRefreshToken() {
        AuthResponse response = tokenService.issueTokens(nimal);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getTokenHash())
                .isNotEqualTo(response.refreshToken())
                .isEqualTo(TokenServiceImpl.hash(response.refreshToken()))
                .hasSize(64);
        assertThat(saved.getUser()).isSameAs(nimal);
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        // 32 random bytes, base64url without padding
        assertThat(response.refreshToken()).hasSize(43).doesNotContain("=", "+", "/");
    }

    @Test
    void refreshRotatesTheToken() {
        when(refreshTokenRepository.findByTokenHash(TokenServiceImpl.hash("old-token")))
                .thenReturn(Optional.of(stored("old-token", nimal)));
        when(refreshTokenRepository.revokeIfActive(5L, NOW)).thenReturn(1);

        AuthResponse response = tokenService.refresh("old-token");

        verify(refreshTokenRepository).revokeIfActive(5L, NOW);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertThat(response.refreshToken()).isNotEqualTo("old-token");
        assertThat(decode(response.accessToken()).getSubject()).isEqualTo("10");
    }

    @Test
    void reusingARevokedTokenFailsAndRevokesEverySession() {
        RefreshToken revoked = stored("old-token", nimal);
        revoked.setRevokedAt(NOW.minus(Duration.ofHours(1)));
        when(refreshTokenRepository.findByTokenHash(TokenServiceImpl.hash("old-token")))
                .thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> tokenService.refresh("old-token")).isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepository).revokeAllForUser(10L, NOW);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void expiredTokenFails() {
        RefreshToken expired = stored("old-token", nimal);
        expired.setExpiresAt(NOW);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> tokenService.refresh("old-token")).isInstanceOf(InvalidTokenException.class);
        verify(refreshTokenRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeAllForUser(anyLong(), any());
    }

    @Test
    void disabledUserCannotRefresh() {
        nimal.setEnabled(false);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored("old-token", nimal)));

        assertThatThrownBy(() -> tokenService.refresh("old-token")).isInstanceOf(InvalidTokenException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void unknownTokenFails() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.refresh("made-up")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void losingAConcurrentRotationFails() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored("old-token", nimal)));
        // Another request revoked it between the read and the update
        when(refreshTokenRepository.revokeIfActive(5L, NOW)).thenReturn(0);

        assertThatThrownBy(() -> tokenService.refresh("old-token")).isInstanceOf(InvalidTokenException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeOnlyAffectsTheCallersOwnToken() {
        when(refreshTokenRepository.findByTokenHash(TokenServiceImpl.hash("nimals-token")))
                .thenReturn(Optional.of(stored("nimals-token", nimal)));

        tokenService.revoke("nimals-token", 99L);
        verify(refreshTokenRepository, never()).revokeIfActive(anyLong(), any());

        tokenService.revoke("nimals-token", 10L);
        verify(refreshTokenRepository).revokeIfActive(eq(5L), eq(NOW));
    }

    @Test
    void revokeAllEndsEverySession() {
        tokenService.revokeAll(10L);

        verify(refreshTokenRepository).revokeAllForUser(10L, NOW);
    }
}
