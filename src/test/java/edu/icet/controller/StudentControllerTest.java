package edu.icet.controller;

import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.request.StudentUpdateRequest;
import edu.icet.dto.response.StudentResponse;
import edu.icet.entity.Gender;
import edu.icet.exception.DuplicateEmailException;
import edu.icet.exception.StudentNotFoundException;
import edu.icet.security.RestAccessDeniedHandler;
import edu.icet.security.StudentSecurity;
import edu.icet.security.WithSecurityConfig;
import edu.icet.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static edu.icet.security.TestTokens.admin;
import static edu.icet.security.TestTokens.lecturer;
import static edu.icet.security.TestTokens.student;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
@WithSecurityConfig
class StudentControllerTest {

    // Student profile 1 belongs to user 10; user 20 is another student
    private static final long OWNER = 10L;
    private static final long OTHER_STUDENT = 20L;

    private static final String CREATE_BODY = """
            {"firstName": "Nimal", "lastName": "Perera", "age": 21, "email": "nimal@example.com",
             "password": "secret123", "institute": "ICET", "batch": "2023", "gender": "MALE"}
            """;
    private static final String UPDATE_BODY = """
            {"firstName": "Nimal", "lastName": "Silva", "age": 22, "email": "nimal@example.com",
             "institute": "ICET", "batch": "2024", "gender": "MALE"}
            """;
    private static final StudentResponse NIMAL = new StudentResponse(
            1L, "Nimal", "Perera", 21, "nimal@example.com", "ICET", "2023", Gender.MALE);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean(name = "studentSecurity")
    private StudentSecurity studentSecurity;

    @BeforeEach
    void ownership() {
        when(studentSecurity.isOwner(eq(1L), any())).thenAnswer(invocation ->
                edu.icet.security.CurrentUser.id((org.springframework.security.core.Authentication)
                        invocation.getArgument(1)) == OWNER);
    }

    private static ResultActions expectForbidden(ResultActions result) throws Exception {
        return result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value(RestAccessDeniedHandler.FORBIDDEN))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Nested
    class WithoutToken {

        @Test
        void everyStudentEndpointReturns401ApiError() throws Exception {
            for (var request : List.of(get("/api/v1/students"), get("/api/v1/students/1"),
                    post("/api/v1/students").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY),
                    put("/api/v1/students/1").contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY),
                    delete("/api/v1/students/1"))) {
                mockMvc.perform(request)
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.status").value(401))
                        .andExpect(jsonPath("$.error").value("Unauthorized"));
            }
            verify(studentService, never()).getById(any());
        }
    }

    @Nested
    class Create {

        @Test
        void adminGets201WithLocationAndBody() throws Exception {
            when(studentService.create(any(StudentCreateRequest.class))).thenReturn(NIMAL);

            mockMvc.perform(post("/api/v1/students").with(admin())
                            .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/api/v1/students/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("nimal@example.com"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        void lecturerAndStudentAreForbidden() throws Exception {
            expectForbidden(mockMvc.perform(post("/api/v1/students").with(lecturer())
                    .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY)));
            expectForbidden(mockMvc.perform(post("/api/v1/students").with(student(OWNER))
                    .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY)));
            verify(studentService, never()).create(any());
        }

        @Test
        void invalidBodyReturns400WithFieldErrors() throws Exception {
            String invalid = """
                    {"firstName": "", "lastName": "Perera", "age": 3, "email": "not-an-email",
                     "password": "short", "institute": "ICET", "batch": "2023", "gender": "MALE"}
                    """;

            mockMvc.perform(post("/api/v1/students").with(admin())
                            .contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.path").value("/api/v1/students"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.fieldErrors", hasSize(5)))
                    .andExpect(jsonPath("$.fieldErrors[*].field").value(
                            containsInAnyOrder("age", "email", "firstName", "password", "password")))
                    // A rejected password must never be echoed back
                    .andExpect(content().string(not(containsString("short"))));
            verify(studentService, never()).create(any());
        }

        @Test
        void malformedJsonReturns400() throws Exception {
            mockMvc.perform(post("/api/v1/students").with(admin())
                            .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
        }

        @Test
        void duplicateEmailReturns409() throws Exception {
            when(studentService.create(any(StudentCreateRequest.class)))
                    .thenThrow(new DuplicateEmailException("nimal@example.com"));

            mockMvc.perform(post("/api/v1/students").with(admin())
                            .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("An account with email nimal@example.com already exists"));
        }
    }

    @Nested
    class GetById {

        @Test
        void adminAndLecturerCanReadAnyProfile() throws Exception {
            when(studentService.getById(1L)).thenReturn(NIMAL);

            mockMvc.perform(get("/api/v1/students/1").with(admin())).andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/students/1").with(lecturer()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Nimal"));
        }

        @Test
        void studentCanReadOwnProfile() throws Exception {
            when(studentService.getById(1L)).thenReturn(NIMAL);

            mockMvc.perform(get("/api/v1/students/1").with(student(OWNER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("nimal@example.com"));
        }

        @Test
        void studentCannotReadSomeoneElsesProfile() throws Exception {
            expectForbidden(mockMvc.perform(get("/api/v1/students/1").with(student(OTHER_STUDENT))));
            verify(studentService, never()).getById(any());
        }

        @Test
        void missingIdIs404ForStaffBut403ForStudents() throws Exception {
            when(studentService.getById(99L)).thenThrow(new StudentNotFoundException(99L));

            mockMvc.perform(get("/api/v1/students/99").with(admin()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Student with id 99 not found"))
                    .andExpect(jsonPath("$.path").value("/api/v1/students/99"));
            // A student can't tell a missing profile from someone else's
            expectForbidden(mockMvc.perform(get("/api/v1/students/99").with(student(OWNER))));
        }

        @Test
        void nonNumericIdReturns400() throws Exception {
            mockMvc.perform(get("/api/v1/students/abc").with(admin()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    class Search {

        @Test
        void lecturerGetsDefaultPagingAndPageShape() throws Exception {
            Pageable expected = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"));
            when(studentService.search(eq("nim"), isNull(), isNull(), eq(expected)))
                    .thenReturn(new PageImpl<>(List.of(NIMAL), expected, 1));

            mockMvc.perform(get("/api/v1/students").param("name", "nim").with(lecturer()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].email").value("nimal@example.com"))
                    .andExpect(jsonPath("$.page.size").value(20))
                    .andExpect(jsonPath("$.page.number").value(0))
                    .andExpect(jsonPath("$.page.totalElements").value(1))
                    .andExpect(jsonPath("$.page.totalPages").value(1));
        }

        @Test
        void adminCanList() throws Exception {
            when(studentService.search(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(NIMAL)));

            mockMvc.perform(get("/api/v1/students").with(admin())).andExpect(status().isOk());
        }

        @Test
        void studentsCannotList() throws Exception {
            expectForbidden(mockMvc.perform(get("/api/v1/students").with(student(OWNER))));
            verify(studentService, never()).search(any(), any(), any(), any());
        }
    }

    @Nested
    class Update {

        @Test
        void ownerCanUpdateOwnProfile() throws Exception {
            when(studentService.update(eq(1L), any(StudentUpdateRequest.class))).thenReturn(NIMAL);

            mockMvc.perform(put("/api/v1/students/1").with(student(OWNER))
                            .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                    .andExpect(status().isOk());
        }

        @Test
        void studentCannotUpdateSomeoneElsesProfile() throws Exception {
            expectForbidden(mockMvc.perform(put("/api/v1/students/1").with(student(OTHER_STUDENT))
                    .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY)));
            verify(studentService, never()).update(any(), any());
        }

        @Test
        void ownerCannotChangeOwnRoleOrEnabledFlag() throws Exception {
            String withRole = UPDATE_BODY.replace("\"gender\": \"MALE\"", "\"gender\": \"MALE\", \"role\": \"ADMIN\"");
            String withEnabled = UPDATE_BODY.replace("\"gender\": \"MALE\"", "\"gender\": \"MALE\", \"enabled\": true");

            expectForbidden(mockMvc.perform(put("/api/v1/students/1").with(student(OWNER))
                    .contentType(MediaType.APPLICATION_JSON).content(withRole)));
            expectForbidden(mockMvc.perform(put("/api/v1/students/1").with(student(OWNER))
                    .contentType(MediaType.APPLICATION_JSON).content(withEnabled)));
            verify(studentService, never()).update(any(), any());
        }

        @Test
        void adminCanChangeRoleAndEnabledFlag() throws Exception {
            when(studentService.update(eq(1L), any(StudentUpdateRequest.class))).thenReturn(NIMAL);
            String body = UPDATE_BODY.replace("\"gender\": \"MALE\"", "\"gender\": \"MALE\", \"enabled\": false");

            mockMvc.perform(put("/api/v1/students/1").with(admin())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
            verify(studentService).update(eq(1L), org.mockito.ArgumentMatchers.argThat(
                    request -> Boolean.FALSE.equals(request.enabled())));
        }

        @Test
        void lecturerCannotUpdate() throws Exception {
            expectForbidden(mockMvc.perform(put("/api/v1/students/1").with(lecturer())
                    .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY)));
        }

        @Test
        void invalidBodyStillReturns400ForOwner() throws Exception {
            mockMvc.perform(put("/api/v1/students/1").with(student(OWNER))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"firstName\": \"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"));
        }
    }

    @Nested
    class Delete {

        @Test
        void adminGets204() throws Exception {
            mockMvc.perform(delete("/api/v1/students/1").with(admin()))
                    .andExpect(status().isNoContent());
            verify(studentService).delete(1L);
        }

        @Test
        void adminGets404WhenMissing() throws Exception {
            doThrow(new StudentNotFoundException(99L)).when(studentService).delete(99L);

            mockMvc.perform(delete("/api/v1/students/99").with(admin()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void ownerAndLecturerCannotDelete() throws Exception {
            expectForbidden(mockMvc.perform(delete("/api/v1/students/1").with(student(OWNER))));
            expectForbidden(mockMvc.perform(delete("/api/v1/students/1").with(lecturer())));
            verify(studentService, never()).delete(any());
        }
    }

    @Test
    void unexpectedErrorReturns500WithoutDetails() throws Exception {
        when(studentService.getById(1L)).thenThrow(new IllegalStateException("db password is hunter2"));

        mockMvc.perform(get("/api/v1/students/1").with(admin()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("hunter2"))));
    }

    @Test
    void corsPreflightAllowsConfiguredOriginWithoutToken() throws Exception {
        mockMvc.perform(options("/api/v1/students")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void corsPreflightAllowsLiveServerOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/students")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5500")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5500"));
    }

    @Test
    void corsPreflightRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/students")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
