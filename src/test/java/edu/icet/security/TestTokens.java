package edu.icet.security;

import edu.icet.entity.Role;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Authenticated MockMvc requests for each role, shaped like the access tokens the app issues. */
public final class TestTokens {

    public static final long ADMIN_USER_ID = 1L;
    public static final long LECTURER_USER_ID = 2L;

    private TestTokens() {
    }

    public static RequestPostProcessor as(long userId, Role role) {
        return jwt()
                .jwt(token -> token
                        .subject(String.valueOf(userId))
                        .claim(JwtConfig.EMAIL_CLAIM, "user" + userId + "@example.com")
                        .claim(JwtConfig.ROLE_CLAIM, role.name()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public static RequestPostProcessor admin() {
        return as(ADMIN_USER_ID, Role.ADMIN);
    }

    public static RequestPostProcessor lecturer() {
        return as(LECTURER_USER_ID, Role.LECTURER);
    }

    public static RequestPostProcessor student(long userId) {
        return as(userId, Role.STUDENT);
    }
}
