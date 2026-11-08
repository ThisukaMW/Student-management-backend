package edu.icet.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the signed-in user's id from the access token's subject claim. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    /** The user id, or null when the request isn't authenticated with a JWT. */
    public static Long id(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return id(jwt);
        }
        return null;
    }
}
