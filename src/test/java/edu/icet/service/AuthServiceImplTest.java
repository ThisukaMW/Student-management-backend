package edu.icet.service;

import edu.icet.dto.request.LoginRequest;
import edu.icet.dto.response.StudentResponse;
import edu.icet.exception.InvalidCredentialsException;
import edu.icet.mapper.StudentMapperImpl;
import edu.icet.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(studentRepository, new StudentMapperImpl());
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        when(studentRepository.findByEmail("nimal@example.com"))
                .thenReturn(Optional.of(StudentServiceImplTest.student(1L, "nimal@example.com")));

        StudentResponse response = authService.login(new LoginRequest("Nimal@Example.com", "secret123"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("nimal@example.com");
    }

    @Test
    void loginFailsWithWrongPassword() {
        when(studentRepository.findByEmail("nimal@example.com"))
                .thenReturn(Optional.of(StudentServiceImplTest.student(1L, "nimal@example.com")));

        assertThatThrownBy(() -> authService.login(new LoginRequest("nimal@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginFailsWithUnknownEmail() {
        when(studentRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "secret123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
