package edu.icet.controller;

import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.response.StudentResponse;
import edu.icet.entity.Gender;
import edu.icet.exception.DuplicateEmailException;
import edu.icet.exception.StudentNotFoundException;
import edu.icet.service.StudentService;
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

import java.util.List;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    private static final String VALID_BODY = """
            {
              "firstName": "Nimal",
              "lastName": "Perera",
              "age": 21,
              "email": "nimal@example.com",
              "password": "secret123",
              "institute": "ICET",
              "batch": "2023",
              "gender": "MALE"
            }
            """;

    private static final StudentResponse NIMAL = new StudentResponse(
            1L, "Nimal", "Perera", 21, "nimal@example.com", "ICET", "2023", Gender.MALE);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        when(studentService.create(any(StudentCreateRequest.class))).thenReturn(NIMAL);

        mockMvc.perform(post("/api/v1/students").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/api/v1/students/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("nimal@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createReturns400WithFieldErrorsForInvalidBody() throws Exception {
        String invalid = """
                {"firstName": "", "lastName": "Perera", "age": 3, "email": "not-an-email",
                 "password": "short", "institute": "ICET", "batch": "2023", "gender": "MALE"}
                """;

        mockMvc.perform(post("/api/v1/students").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/v1/students"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors", hasSize(4)))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(
                        org.hamcrest.Matchers.containsInAnyOrder("age", "email", "firstName", "password")))
                // A rejected password must never be echoed back
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("short"))));
        verify(studentService, never()).create(any());
    }

    @Test
    void createReturns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/students").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void createReturns409ForDuplicateEmail() throws Exception {
        when(studentService.create(any(StudentCreateRequest.class)))
                .thenThrow(new DuplicateEmailException("nimal@example.com"));

        mockMvc.perform(post("/api/v1/students").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("An account with email nimal@example.com already exists"));
    }

    @Test
    void getByIdReturns200() throws Exception {
        when(studentService.getById(1L)).thenReturn(NIMAL);

        mockMvc.perform(get("/api/v1/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Nimal"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(studentService.getById(99L)).thenThrow(new StudentNotFoundException(99L));

        mockMvc.perform(get("/api/v1/students/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student with id 99 not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/students/99"));
    }

    @Test
    void getByIdReturns400ForNonNumericId() throws Exception {
        mockMvc.perform(get("/api/v1/students/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void searchUsesDefaultPagingAndReturnsPageShape() throws Exception {
        Pageable expected = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"));
        when(studentService.search(eq("nim"), isNull(), isNull(), eq(expected)))
                .thenReturn(new PageImpl<>(List.of(NIMAL), expected, 1));

        mockMvc.perform(get("/api/v1/students").param("name", "nim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value("nimal@example.com"))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/students/1"))
                .andExpect(status().isNoContent());
        verify(studentService).delete(1L);
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        doThrow(new StudentNotFoundException(99L)).when(studentService).delete(99L);

        mockMvc.perform(delete("/api/v1/students/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unexpectedErrorReturns500WithoutDetails() throws Exception {
        when(studentService.getById(1L)).thenThrow(new IllegalStateException("db password is hunter2"));

        mockMvc.perform(get("/api/v1/students/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("hunter2"))));
    }

    @Test
    void corsPreflightAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/students")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
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
