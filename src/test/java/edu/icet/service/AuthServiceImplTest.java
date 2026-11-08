package edu.icet.service;

import edu.icet.dto.request.ChangePasswordRequest;
import edu.icet.dto.request.LoginRequest;
import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.response.AuthResponse;
import edu.icet.dto.response.MeResponse;
import edu.icet.dto.response.StudentResponse;
import edu.icet.dto.response.UserSummary;
import edu.icet.entity.Gender;
import edu.icet.entity.Role;
import edu.icet.entity.User;
import edu.icet.exception.IncorrectPasswordException;
import edu.icet.exception.InvalidCredentialsException;
import edu.icet.exception.InvalidTokenException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentService studentService;

    @Mock
    private TokenService tokenService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, studentRepository, new StudentMapperImpl(), ENCODER,
                studentService, tokenService);
    }

    private static AuthResponse tokensFor(User user) {
        return new AuthResponse("access", "refresh", "Bearer", 900,
                new UserSummary(user.getId(), user.getEmail(), user.getRole()));
    }

    @Test
    void registerCreatesAStudentAccountAndProfile() {
        StudentCreateRequest request = new StudentCreateRequest(
                "Nimal", "Perera", 21, "nimal@example.com", "secret123", "ICET", "2023", Gender.MALE);
        StudentResponse created = new StudentResponse(
                1L, "Nimal", "Perera", 21, "nimal@example.com", "ICET", "2023", Gender.MALE);
        when(studentService.create(request)).thenReturn(created);

        assertThat(authService.register(request)).isEqualTo(created);
    }

    @Test
    void loginIssuesTokensForCorrectCredentials() {
        User account = user(10L, "nimal@example.com", Role.STUDENT);
        when(userRepository.findByEmail("nimal@example.com")).thenReturn(Optional.of(account));
        when(tokenService.issueTokens(account)).thenReturn(tokensFor(account));

        AuthResponse response = authService.login(new LoginRequest("Nimal@Example.com", "secret123"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.user().email()).isEqualTo("nimal@example.com");
    }

    @Test
    void loginFailsWithWrongPassword() {
        when(userRepository.findByEmail("nimal@example.com"))
                .thenReturn(Optional.of(user(10L, "nimal@example.com", Role.STUDENT)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("nimal@example.com", "wrong-password1")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        verify(tokenService, never()).issueTokens(any());
    }

    @Test
    void loginFailsWithUnknownEmailWithSameMessage() {
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
        verify(tokenService, never()).issueTokens(any());
    }

    @Test
    void refreshAndLogoutDelegateToTokenService() {
        User account = user(10L, "nimal@example.com", Role.STUDENT);
        when(tokenService.refresh("refresh")).thenReturn(tokensFor(account));

        assertThat(authService.refresh("refresh").accessToken()).isEqualTo("access");

        authService.logout("refresh", 10L);
        verify(tokenService).revoke("refresh", 10L);
    }

    @Test
    void meIncludesTheStudentProfileWhenThereIsOne() {
        User account = user(10L, "nimal@example.com", Role.STUDENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(account));
        when(studentRepository.findByUserId(10L)).thenReturn(Optional.of(student(1L, account)));

        MeResponse me = authService.me(10L);

        assertThat(me.email()).isEqualTo("nimal@example.com");
        assertThat(me.role()).isEqualTo(Role.STUDENT);
        assertThat(me.student()).isNotNull();
        assertThat(me.student().id()).isEqualTo(1L);
    }

    @Test
    void meHasNoProfileForStaff() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "lecturer@example.com", Role.LECTURER)));
        when(studentRepository.findByUserId(2L)).thenReturn(Optional.empty());

        MeResponse me = authService.me(2L);

        assertThat(me.role()).isEqualTo(Role.LECTURER);
        assertThat(me.student()).isNull();
    }

    @Test
    void meForADeletedAccountIsUnauthorized() {
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(10L)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void changePasswordStoresNewHashAndEndsAllSessions() {
        User account = user(10L, "nimal@example.com", Role.STUDENT);
        String oldHash = account.getPasswordHash();
        when(userRepository.findById(10L)).thenReturn(Optional.of(account));

        authService.changePassword(10L, new ChangePasswordRequest("secret123", "newSecret456"));

        assertThat(account.getPasswordHash()).isNotEqualTo(oldHash).isNotEqualTo("newSecret456");
        assertThat(ENCODER.matches("newSecret456", account.getPasswordHash())).isTrue();
        verify(tokenService).revokeAll(10L);
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        User account = user(10L, "nimal@example.com", Role.STUDENT);
        String oldHash = account.getPasswordHash();
        when(userRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> authService.changePassword(10L,
                new ChangePasswordRequest("wrong-password1", "newSecret456")))
                .isInstanceOf(IncorrectPasswordException.class)
                .extracting("field").isEqualTo("currentPassword");
        assertThat(account.getPasswordHash()).isEqualTo(oldHash);
        verify(tokenService, never()).revokeAll(any());
    }

    @Test
    void changePasswordRejectsReusingTheCurrentPassword() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L, "nimal@example.com", Role.STUDENT)));

        assertThatThrownBy(() -> authService.changePassword(10L, new ChangePasswordRequest("secret123", "secret123")))
                .isInstanceOf(IncorrectPasswordException.class)
                .extracting("field").isEqualTo("newPassword");
    }
}
