package edu.icet.dto.response;

import edu.icet.entity.Gender;

/** Public view of a student. Never contains the password. */
public record StudentResponse(
        Long id,
        String firstName,
        String lastName,
        Integer age,
        String email,
        String institute,
        String batch,
        Gender gender
) {
}
