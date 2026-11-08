package edu.icet.service;

import edu.icet.dto.response.AuthResponse;
import edu.icet.dto.response.UserSummary;
import edu.icet.entity.RefreshToken;
import edu.icet.entity.User;
import edu.icet.exception.InvalidTokenException;
import edu.icet.repository.RefreshTokenRepository;
import edu.icet.security.JwtConfig;
import edu.icet.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TokenServiceImpl implements TokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    @Override
    public AuthResponse issueTokens(User user) {
        Instant now = clock.instant();
        String accessToken = accessToken(user, now);
        String refreshToken = newRefreshToken(user, now);
        return new AuthResponse(accessToken, refreshToken, AuthResponse.BEARER,
                jwtProperties.accessTokenTtl().toSeconds(),
                new UserSummary(user.getId(), user.getEmail(), user.getRole()));
    }

    // No rollback on InvalidTokenException: the "revoke everything on reuse" below must survive the exception
    @Override
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResponse refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(InvalidTokenException::new);
        User user = stored.getUser();

        if (stored.isRevoked()) {
            // A rotated-out or logged-out token is being used again: assume it leaked and end every session
            log.warn("Revoked refresh token reused for user {}; revoking all of their refresh tokens", user.getId());
            refreshTokenRepository.revokeAllForUser(user.getId(), now);
            throw new InvalidTokenException();
        }
        if (stored.isExpired(now) || !user.isEnabled()) {
            throw new InvalidTokenException();
        }
        if (refreshTokenRepository.revokeIfActive(stored.getId(), now) == 0) {
            // Another request rotated this token a moment ago
            throw new InvalidTokenException();
        }
        return issueTokens(user);
    }

    @Override
    public void revoke(String rawRefreshToken, Long userId) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .filter(token -> token.getUser().getId().equals(userId))
                .ifPresent(token -> refreshTokenRepository.revokeIfActive(token.getId(), clock.instant()));
    }

    @Override
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId, clock.instant());
    }

    private String accessToken(User user, Instant now) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.accessTokenTtl()))
                .claim(JwtConfig.EMAIL_CLAIM, user.getEmail())
                .claim(JwtConfig.ROLE_CLAIM, user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String newRefreshToken(User user, Instant now) {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(raw))
                .createdAt(now)
                .expiresAt(now.plus(jwtProperties.refreshTokenTtl()))
                .build());
        return raw;
    }

    // SHA-256 rather than BCrypt: the token is 256 random bits, so a fast hash is safe and allows an indexed lookup
    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
