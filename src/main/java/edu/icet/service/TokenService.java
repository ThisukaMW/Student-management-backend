package edu.icet.service;

import edu.icet.dto.response.AuthResponse;
import edu.icet.entity.User;

/** Issues access tokens (JWT) and manages refresh tokens (opaque, stored hashed, rotated). */
public interface TokenService {

    /** A new access token and refresh token for a user who just proved their identity. */
    AuthResponse issueTokens(User user);

    /**
     * Exchanges a refresh token for a new pair and revokes the old one. Presenting a token that was
     * already revoked revokes all of that user's refresh tokens, since it suggests the token was stolen.
     */
    AuthResponse refresh(String rawRefreshToken);

    /** Revokes the token if it belongs to the given user; anything else is silently ignored. */
    void revoke(String rawRefreshToken, Long userId);

    /** Logs the user out everywhere. */
    void revokeAll(Long userId);
}
