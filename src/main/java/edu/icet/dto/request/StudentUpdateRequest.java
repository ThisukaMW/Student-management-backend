package edu.icet.dto.request;

import edu.icet.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Full replacement of a student's profile. The password is deliberately absent:
 * password changes get their own flow in Phase 2.
 */
public record StudentUpdateRequest(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @NotNull @Min(15) @Max(100) Integer age,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 100) String institute,
        @NotBlank @Size(max = 100) String batch,
        @NotNull Gender gender
) {
}
