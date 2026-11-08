package edu.icet.service;

import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.response.StudentResponse;
import edu.icet.entity.Gender;
import edu.icet.entity.Role;
import edu.icet.entity.Student;
import edu.icet.entity.User;
import edu.icet.exception.DuplicateEmailException;
import edu.icet.mapper.StudentMapperImpl;
import edu.icet.repository.StudentRepository;
import edu.icet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static edu.icet.service.TestData.ENCODER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** register() through the real service chain (auth -> student -> user); only the repositories are mocked. */
@ExtendWith(MockitoExtension.class)
class RegistrationFlowTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TokenService tokenService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        StudentMapperImpl mapper = new StudentMapperImpl();
        UserServiceImpl userService = new UserServiceImpl(userRepository, ENCODER, tokenService);
        StudentServiceImpl studentService = new StudentServiceImpl(studentRepository, mapper, userService);
        authService = new AuthServiceImpl(userRepository, studentRepository, mapper, ENCODER, studentService,
                tokenService);
    }

    private static StudentCreateRequest request(String email) {
        return new StudentCreateRequest("Nimal", "Perera", 21, email, "Secret123", "ICET", "2023", Gender.MALE);
    }

    @Test
    void registerHashesThePasswordAndCreatesAStudentAccount() {
        when(userRepository.existsByEmail("nimal@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        StudentResponse response = authService.register(request("Nimal@Example.com"));

        ArgumentCaptor<User> account = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(account.capture());
        assertThat(account.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(account.getValue().getEmail()).isEqualTo("nimal@example.com");
        assertThat(account.getValue().getPasswordHash()).isNotEqualTo("Secret123").startsWith("$2a$");
        assertThat(ENCODER.matches("Secret123", account.getValue().getPasswordHash())).isTrue();

        ArgumentCaptor<Student> profile = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(profile.capture());
        assertThat(profile.getValue().getUser()).isSameAs(account.getValue());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("nimal@example.com");
    }

    @Test
    void registerRejectsADuplicateEmailBeforeSavingAnything() {
        when(userRepository.existsByEmail("nimal@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request("NIMAL@example.com")))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("An account with email nimal@example.com already exists");
        verify(userRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }
}
