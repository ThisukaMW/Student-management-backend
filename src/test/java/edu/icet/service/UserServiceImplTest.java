package edu.icet.service;

import edu.icet.entity.Role;
import edu.icet.entity.User;
import edu.icet.exception.DuplicateEmailException;
import edu.icet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static edu.icet.service.TestData.ENCODER;
import static edu.icet.service.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, ENCODER);
    }

    @Test
    void createUserHashesPasswordAndNormalizesEmail() {
        when(userRepository.existsByEmail("nimal@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createUser(" Nimal@Example.com ", "secret123", Role.STUDENT);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("nimal@example.com");
        assertThat(saved.getPasswordHash()).isNotEqualTo("secret123").startsWith("$2a$");
        assertThat(ENCODER.matches("secret123", saved.getPasswordHash())).isTrue();
        assertThat(saved.getRole()).isEqualTo(Role.STUDENT);
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("nimal@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser("nimal@example.com", "secret123", Role.STUDENT))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeEmailUpdatesNormalizedEmail() {
        User account = user(10L, "nimal@example.com", Role.STUDENT);
        when(userRepository.existsByEmailAndIdNot("nimal.silva@example.com", 10L)).thenReturn(false);

        userService.changeEmail(account, "Nimal.Silva@Example.com");

        assertThat(account.getEmail()).isEqualTo("nimal.silva@example.com");
    }

    @Test
    void changeEmailToSameAddressIsANoOp() {
        User account = user(10L, "nimal@example.com", Role.STUDENT);

        userService.changeEmail(account, "NIMAL@example.com");

        assertThat(account.getEmail()).isEqualTo("nimal@example.com");
        verify(userRepository, never()).existsByEmailAndIdNot(any(), any());
    }

    @Test
    void changeEmailRejectsAddressOfAnotherAccount() {
        User account = user(10L, "nimal@example.com", Role.STUDENT);
        when(userRepository.existsByEmailAndIdNot("kamal@example.com", 10L)).thenReturn(true);

        assertThatThrownBy(() -> userService.changeEmail(account, "kamal@example.com"))
                .isInstanceOf(DuplicateEmailException.class);
        assertThat(account.getEmail()).isEqualTo("nimal@example.com");
    }
}
