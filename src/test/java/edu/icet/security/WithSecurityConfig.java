package edu.icet.security;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Loads the real security setup into a {@code @WebMvcTest} slice: filter chain, JWT encoder/decoder,
 * method security and the ApiError 401/403 handlers, with a test-only signing secret.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({SecurityConfig.class, JwtConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ApiErrorWriter.class})
@TestPropertySource(properties = "app.security.jwt.secret=test-only-secret-that-is-long-enough-123456")
public @interface WithSecurityConfig {
}
