package edu.icet.controller;

import edu.icet.dto.request.CreateUserRequest;
import edu.icet.dto.response.UserResponse;
import edu.icet.entity.Role;
import edu.icet.exception.DuplicateEmailException;
import edu.icet.exception.FieldValidationException;
import edu.icet.security.RestAccessDeniedHandler;
import edu.icet.security.WithSecurityConfig;
import edu.icet.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static edu.icet.security.TestTokens.admin;
import static edu.icet.security.TestTokens.lecturer;
import static edu.icet.security.TestTokens.student;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@WithSecurityConfig
class AdminUserControllerTest {

    private static final String BODY = """
            {"email": "lecturer@example.com", "password": "Teach1234", "role": "LECTURER"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    void adminCreatesStaffAccount() throws Exception {
        when(adminUserService.createStaffUser(any(CreateUserRequest.class))).thenReturn(new UserResponse(
                2L, "lecturer@example.com", Role.LECTURER, true, Instant.parse("2026-11-08T10:00:00Z")));

        mockMvc.perform(post("/api/v1/admin/users").with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.role").value("LECTURER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void lecturersAndStudentsAreForbidden() throws Exception {
        for (var caller : java.util.List.of(lecturer(), student(10L))) {
            mockMvc.perform(post("/api/v1/admin/users").with(caller)
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(RestAccessDeniedHandler.FORBIDDEN));
        }
        verify(adminUserService, never()).createStaffUser(any());
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentRoleIsRejectedAsFieldError() throws Exception {
        when(adminUserService.createStaffUser(any())).thenThrow(
                new FieldValidationException("role", "must be ADMIN or LECTURER; create students with POST /api/v1/students"));

        mockMvc.perform(post("/api/v1/admin/users").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("LECTURER", "STUDENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("role"));
    }

    @Test
    void weakPasswordAndMissingRoleAreValidated() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"lecturer@example.com\", \"password\": \"weakpass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field").value(
                        org.hamcrest.Matchers.containsInAnyOrder("password", "role")));
        verify(adminUserService, never()).createStaffUser(any());
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        when(adminUserService.createStaffUser(any())).thenThrow(new DuplicateEmailException("lecturer@example.com"));

        mockMvc.perform(post("/api/v1/admin/users").with(admin()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }
}
