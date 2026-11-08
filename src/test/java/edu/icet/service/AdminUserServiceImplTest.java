package edu.icet.service;

import edu.icet.dto.request.CreateUserRequest;
import edu.icet.dto.response.UserResponse;
import edu.icet.entity.Role;
import edu.icet.exception.FieldValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static edu.icet.service.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserService userService;

    private AdminUserServiceImpl adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl(userService);
    }

    @Test
    void createsLecturerAccount() {
        when(userService.createUser("lecturer@example.com", "Teach1234", Role.LECTURER))
                .thenReturn(user(2L, "lecturer@example.com", Role.LECTURER));

        UserResponse response = adminUserService.createStaffUser(
                new CreateUserRequest("lecturer@example.com", "Teach1234", Role.LECTURER));

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.role()).isEqualTo(Role.LECTURER);
        assertThat(response.enabled()).isTrue();
    }

    @Test
    void rejectsStudentRole() {
        assertThatThrownBy(() -> adminUserService.createStaffUser(
                new CreateUserRequest("s@example.com", "Secret123", Role.STUDENT)))
                .isInstanceOf(FieldValidationException.class)
                .extracting("field").isEqualTo("role");
        verify(userService, never()).createUser(any(), any(), any());
    }
}
