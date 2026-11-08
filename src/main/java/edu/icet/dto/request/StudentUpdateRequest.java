package edu.icet.dto.request;

import edu.icet.entity.Gender;

/**
 * Full replacement of a student's profile. The password is deliberately absent:
 * password changes get their own flow in Phase 2.
 */
public record StudentUpdateRequest(
        String firstName,
        String lastName,
        Integer age,
        String email,
        String institute,
        String batch,
        Gender gender
) {
}
