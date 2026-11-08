package edu.icet.controller;

import edu.icet.dto.request.ChangePasswordRequest;
import edu.icet.dto.request.LoginRequest;
import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.response.AuthResponse;
import edu.icet.dto.response.MeResponse;
import edu.icet.dto.response.StudentResponse;
import edu.icet.dto.response.UserSummary;
import edu.icet.entity.Gender;
import edu.icet.entity.Role;
import edu.icet.exception.DuplicateEmailException;
import edu.icet.exception.IncorrectPasswordException;
import edu.icet.exception.InvalidCredentialsException;
import edu.icet.exception.InvalidTokenException;
import edu.icet.security.WithSecurityConfig;
import edu.icet.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static edu.icet.security.TestTokens.admin;
import static edu.icet.security.TestTokens.student;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@WithSecurityConfig
class AuthControllerTest {

    private static final String REGISTER_BODY = """
            {"firstName": "Nimal", "lastName": "Perera", "age": 21, "email": "nimal@example.com",
             "password": "secret123", "institute": "ICET", "batch": "2023", "gender": "MALE"}
            """;
    private static final String LOGIN_BODY = """
            {"email": "nimal@example.com", "password": "secret123"}
            """;
    private static final String REFRESH_BODY = """
            {"refreshToken": "the-refresh-token"}
            """;
    private static final StudentResponse NIMAL = new StudentResponse(
            1L, "Nimal", "Perera", 21, "nimal@example.com", "ICET", "2023", Gender.MALE);
    private static final AuthResponse TOKENS = new AuthResponse("the-access-token", "new-refresh-token", "Bearer",
            900, new UserSummary(10L, "nimal@example.com", Role.STUDENT));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    // --- register ---

    @Test
    void registerIsPublicAndReturns201WithProfile() throws Exception {
        when(authService.register(any(StudentCreateRequest.class))).thenReturn(NIMAL);

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/api/v1/students/1"))
                .andExpect(jsonPath("$.email").value("nimal@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerRejectsWeakPasswordWithoutEchoingIt() throws Exception {
        String weak = REGISTER_BODY.replace("secret123", "onlyletters");

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(weak))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must contain at least one digit"))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("onlyletters"))));
        verify(authService, never()).register(any());
    }

    @Test
    void registerWithTakenEmailReturns409() throws Exception {
        when(authService.register(any())).thenThrow(new DuplicateEmailException("nimal@example.com"));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isConflict());
    }

    // --- login ---

    @Test
    void loginIsPublicAndReturnsTokens() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(TOKENS);

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("the-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.id").value(10))
                .andExpect(jsonPath("$.user.role").value("STUDENT"));
    }

    @Test
    void loginReturns401ForBadCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginReturns400WhenEmailMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    // --- refresh ---

    @Test
    void refreshIsPublicAndReturnsANewPair() throws Exception {
        when(authService.refresh("the-refresh-token")).thenReturn(TOKENS);

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(REFRESH_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void refreshWithInvalidTokenReturns401() throws Exception {
        when(authService.refresh("the-refresh-token")).thenThrow(new InvalidTokenException());

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(REFRESH_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("The refresh token is invalid or has expired"));
    }

    // --- logout ---

    @Test
    void logoutRevokesTheCallersToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").with(student(10L))
                        .contentType(MediaType.APPLICATION_JSON).content(REFRESH_BODY))
                .andExpect(status().isNoContent());
        verify(authService).logout("the-refresh-token", 10L);
    }

    @Test
    void logoutRequiresAnAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content(REFRESH_BODY))
                .andExpect(status().isUnauthorized());
        verify(authService, never()).logout(any(), any());
    }

    // --- me ---

    @Test
    void meReturnsTheCurrentAccountAndProfile() throws Exception {
        when(authService.me(10L)).thenReturn(new MeResponse(
                10L, "nimal@example.com", Role.STUDENT, true, Instant.parse("2026-11-08T10:00:00Z"), NIMAL));

        mockMvc.perform(get("/api/v1/auth/me").with(student(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.student.id").value(1))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void meShowsNullProfileForStaff() throws Exception {
        when(authService.me(1L)).thenReturn(new MeResponse(
                1L, "admin@example.com", Role.ADMIN, true, Instant.parse("2026-11-08T10:00:00Z"), null));

        mockMvc.perform(get("/api/v1/auth/me").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student").isEmpty());
    }

    @Test
    void meRequiresAnAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // --- change password ---

    @Test
    void changePasswordReturns204() throws Exception {
        mockMvc.perform(put("/api/v1/auth/me/password").with(student(10L)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"secret123\", \"newPassword\": \"newSecret456\"}"))
                .andExpect(status().isNoContent());
        verify(authService).changePassword(eq(10L), any(ChangePasswordRequest.class));
    }

    @Test
    void changePasswordWithWrongCurrentPasswordReturns400FieldError() throws Exception {
        doThrow(new IncorrectPasswordException("currentPassword", "is incorrect"))
                .when(authService).changePassword(eq(10L), any());

        mockMvc.perform(put("/api/v1/auth/me/password").with(student(10L)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"wrong-pass1\", \"newPassword\": \"newSecret456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currentPassword"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("is incorrect"));
    }

    @Test
    void changePasswordValidatesTheNewPassword() throws Exception {
        mockMvc.perform(put("/api/v1/auth/me/password").with(student(10L)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"secret123\", \"newPassword\": \"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("newPassword"))));
        verify(authService, never()).changePassword(any(), any());
    }

    @Test
    void oldGetLoginEndpointIsGone() throws Exception {
        mockMvc.perform(get("/information/nimal@example.com/secret123").with(admin()))
                .andExpect(status().isNotFound());
    }
}
