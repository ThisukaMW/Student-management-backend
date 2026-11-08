package edu.icet.dto.response;

import edu.icet.entity.Role;

public record UserSummary(
        Long id,
        String email,
        Role role
) {
}
