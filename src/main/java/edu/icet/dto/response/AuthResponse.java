package edu.icet.dto.response;

/**
 * Returned by login and refresh. Send {@code accessToken} as {@code Authorization: Bearer <token>};
 * {@code expiresIn} is the access token's lifetime in seconds.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
    public static final String BEARER = "Bearer";
}
