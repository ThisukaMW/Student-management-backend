package edu.icet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
    public LoginRequest {
        // Trim before validation so " a@b.com " passes @Email
        email = email == null ? null : email.trim();
    }
}
