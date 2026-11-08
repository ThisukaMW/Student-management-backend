package edu.icet.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.icet.entity.Role;

import java.time.Instant;

/** The signed-in account. {@code student} is null for admins and lecturers, who have no profile. */
public record MeResponse(
        Long id,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt,
        @JsonInclude(JsonInclude.Include.ALWAYS) StudentResponse student
) {
}
