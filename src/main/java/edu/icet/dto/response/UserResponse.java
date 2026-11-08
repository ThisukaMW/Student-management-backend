package edu.icet.dto.response;

import edu.icet.entity.Role;
import edu.icet.entity.User;

import java.time.Instant;

/** Public view of an account. Never contains the password hash. */
public record UserResponse(
        Long id,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.isEnabled(), user.getCreatedAt());
    }
}
