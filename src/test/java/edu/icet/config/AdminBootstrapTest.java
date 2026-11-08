package edu.icet.config;

import edu.icet.entity.Role;
import edu.icet.exception.DuplicateEmailException;
import edu.icet.repository.UserRepository;
import edu.icet.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AdminBootstrapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(new AdminProperties(email, password), userRepository, userService);
    }

    @Test
    void createsTheFirstAdminFromConfiguration(CapturedOutput output) {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        bootstrap("Admin@Example.com", "AdminPass123").run(null);

        verify(userService).createUser("Admin@Example.com", "AdminPass123", Role.ADMIN);
        assertThat(output).contains("Created the first ADMIN account: admin@example.com")
                .doesNotContain("AdminPass123");
    }

    @Test
    void doesNothingWhenAnAdminAlreadyExists() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        bootstrap("admin@example.com", "AdminPass123").run(null);

        verify(userService, never()).createUser(any(), any(), any());
    }

    @Test
    void warnsAndSkipsWhenCredentialsAreMissing(CapturedOutput output) {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        bootstrap(null, "").run(null);

        verify(userService, never()).createUser(any(), any(), any());
        assertThat(output).contains("ADMIN_EMAIL / ADMIN_PASSWORD are not set");
    }

    @Test
    void warnsAndSkipsWeakPasswordWithoutLoggingIt(CapturedOutput output) {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        bootstrap("admin@example.com", "weakpass").run(null);

        verify(userService, never()).createUser(any(), any(), any());
        assertThat(output).contains("must contain at least one digit").doesNotContain("weakpass");
    }

    @Test
    void warnsAndSkipsInvalidEmail(CapturedOutput output) {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        bootstrap("not-an-email", "AdminPass123").run(null);

        verify(userService, never()).createUser(any(), any(), any());
        assertThat(output).contains("is not an email address");
    }

    @Test
    void emailTakenByAnotherAccountDoesNotFailStartup(CapturedOutput output) {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userService.createUser(any(), any(), any())).thenThrow(new DuplicateEmailException("admin@example.com"));

        assertThatCode(() -> bootstrap("admin@example.com", "AdminPass123").run(null)).doesNotThrowAnyException();
        assertThat(output).contains("already belongs to a non-admin account");
    }
}
