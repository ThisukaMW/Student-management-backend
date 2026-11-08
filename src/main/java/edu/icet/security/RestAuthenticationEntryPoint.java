package edu.icet.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 401 for a missing, invalid or expired access token, as an ApiError instead of an empty body. */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String MISSING_TOKEN = "Authentication is required to access this resource";
    public static final String INVALID_TOKEN = "The access token is invalid or has expired";

    // Sets the standard "WWW-Authenticate: Bearer ..." header
    private final BearerTokenAuthenticationEntryPoint bearerHeaders = new BearerTokenAuthenticationEntryPoint();
    private final ApiErrorWriter apiErrorWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        bearerHeaders.commence(request, response, ex);
        // Fixed messages: the exception's own text can include token details
        String message = ex instanceof InvalidBearerTokenException ? INVALID_TOKEN : MISSING_TOKEN;
        apiErrorWriter.write(request, response, HttpStatus.UNAUTHORIZED, message);
    }
}
