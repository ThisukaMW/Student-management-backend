package edu.icet.dto.request;

import edu.icet.entity.Gender;
import edu.icet.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Full replacement of a student's profile. The password is deliberately absent: it changes through
 * PUT /api/v1/auth/me/password. {@code role} and {@code enabled} are optional (null = unchanged) and
 * only an ADMIN may send them; an owner sending either gets 403.
 */
public record StudentUpdateRequest(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @NotNull @Min(15) @Max(100) Integer age,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 100) String institute,
        @NotBlank @Size(max = 100) String batch,
        @NotNull Gender gender,
        Role role,
        Boolean enabled
) {
    public StudentUpdateRequest {
        // Trim before validation so " a@b.com " passes @Email
        email = email == null ? null : email.trim();
    }

    /** Profile-only update, as a student editing their own profile would send. */
    public StudentUpdateRequest(String firstName, String lastName, Integer age, String email, String institute,
                                String batch, Gender gender) {
        this(firstName, lastName, age, email, institute, batch, gender, null, null);
    }

    public boolean changesAccountSettings() {
        return role != null || enabled != null;
    }
}
