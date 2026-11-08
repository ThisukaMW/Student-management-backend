package edu.icet.dto.request;

import edu.icet.entity.Gender;

public record StudentCreateRequest(
        String firstName,
        String lastName,
        Integer age,
        String email,
        String password,
        String institute,
        String batch,
        Gender gender
) {
}
