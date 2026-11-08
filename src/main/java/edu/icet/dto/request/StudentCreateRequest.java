package edu.icet.dto.request;

import edu.icet.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentCreateRequest(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @NotNull @Min(15) @Max(100) Integer age,
        @NotBlank @Email @Size(max = 254) String email,
        // 72 is the BCrypt input limit, so Phase 2 hashing won't need a different rule
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String institute,
        @NotBlank @Size(max = 100) String batch,
        @NotNull Gender gender
) {
    public StudentCreateRequest {
        // Trim before validation so " a@b.com " passes @Email
        email = email == null ? null : email.trim();
    }
}
