package edu.icet.dto.request;

import edu.icet.entity.Role;
import edu.icet.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin-only: a staff account. {@code role} must be ADMIN or LECTURER; students come with a profile instead. */
public record CreateUserRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @ValidPassword String password,
        @NotNull Role role
) {
    public CreateUserRequest {
        // Trim before validation so " a@b.com " passes @Email
        email = email == null ? null : email.trim();
    }
}
