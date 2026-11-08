package edu.icet.security;

import edu.icet.controller.StudentController;
import edu.icet.dto.response.StudentResponse;
import edu.icet.entity.Gender;
import edu.icet.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The filter chain with real, signed tokens: what gets in, what is rejected, and the error shape. */
@WebMvcTest(StudentController.class)
@WithSecurityConfig
class SecurityConfigTest {

    private static final StudentResponse NIMAL = new StudentResponse(
            1L, "Nimal", "Perera", 21, "nimal@example.com", "ICET", "2023", Gender.MALE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private StudentService studentService;

    private String token(String issuer, String role, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("1")
                .issuedAt(expiresAt.minus(15, ChronoUnit.MINUTES))
                .expiresAt(expiresAt)
                .claim(JwtConfig.EMAIL_CLAIM, "admin@example.com")
                .claim(JwtConfig.ROLE_CLAIM, role)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Test
    void requestWithoutTokenGets401ApiError() throws Exception {
        mockMvc.perform(get("/api/v1/students/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(RestAuthenticationEntryPoint.MISSING_TOKEN))
                .andExpect(jsonPath("$.path").value("/api/v1/students/1"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void garbageTokenGets401() throws Exception {
        mockMvc.perform(get("/api/v1/students/1").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(RestAuthenticationEntryPoint.INVALID_TOKEN));
    }

    @Test
    void validSignedTokenIsAccepted() throws Exception {
        when(studentService.getById(1L)).thenReturn(NIMAL);
        String token = token("student-management", "ADMIN", Instant.now().plus(15, ChronoUnit.MINUTES));

        mockMvc.perform(get("/api/v1/students/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nimal@example.com"));
    }

    @Test
    void expiredTokenGets401() throws Exception {
        String token = token("student-management", "ADMIN", Instant.now().minus(5, ChronoUnit.MINUTES));

        mockMvc.perform(get("/api/v1/students/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(RestAuthenticationEntryPoint.INVALID_TOKEN));
    }

    @Test
    void tokenFromAnotherIssuerGets401() throws Exception {
        String token = token("someone-else", "ADMIN", Instant.now().plus(15, ChronoUnit.MINUTES));

        mockMvc.perform(get("/api/v1/students/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKeyGets401() throws Exception {
        // Valid shape and claims, but signed with a different secret
        String forged = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJzdHVkZW50LW1hbmFnZW1lbnQiLCJzdWIiOiIxIiwicm9sZSI6IkFETUlOIiwiZXhwIjo0MTAyNDQ0ODAwfQ."
                + "c2lnbmVkLXdpdGgtYW5vdGhlci1rZXktMDEyMzQ1Njc4OWFi";

        mockMvc.perform(get("/api/v1/students/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }
}
