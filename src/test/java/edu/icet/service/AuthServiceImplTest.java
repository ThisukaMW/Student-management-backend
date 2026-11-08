package edu.icet.service;

import edu.icet.dto.request.LoginRequest;
import edu.icet.dto.response.StudentResponse;
import edu.icet.entity.Role;
import edu.icet.entity.User;
import edu.icet.exception.InvalidCredentialsException;
import edu.icet.mapper.StudentMapperImpl;
import edu.icet.repository.StudentRepository;
import edu.icet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static edu.icet.service.TestData.ENCODER;
import static edu.icet.service.TestData.student;
import static edu.icet.service.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, studentRepository, new StudentMapperImpl(), ENCODER);
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        User account = user(10L, "nimal@example.com", Role.STUDENT);
        when(userRepository.findByEmail("nimal@example.com")).thenReturn(Optional.of(account));
        when(studentRepository.findByUserId(10L)).thenReturn(Optional.of(student(1L, account)));

        StudentResponse response = authService.login(new LoginRequest("Nimal@Example.com", "secret123"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("nimal@example.com");
    }

    @Test
    void loginFailsWithWrongPassword() {
        when(userRepository.findByEmail("nimal@example.com"))
                .thenReturn(Optional.of(user(10L, "nimal@example.com", Role.STUDENT)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("nimal@example.com", "wrong-password1")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginFailsWithUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "secret123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginFailsForDisabledAccountWithSameMessage() {
        User account = user(10L, "nimal@example.com", Role.STUDENT);
        account.setEnabled(false);
        when(userRepository.findByEmail("nimal@example.com")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> authService.login(new LoginRequest("nimal@example.com", "secret123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
