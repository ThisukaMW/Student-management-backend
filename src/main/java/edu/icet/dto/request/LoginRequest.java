package edu.icet.dto.request;

public record LoginRequest(
        String email,
        String password
) {
}
