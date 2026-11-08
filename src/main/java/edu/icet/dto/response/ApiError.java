package edu.icet.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * The single error body returned by every endpoint. {@code fieldErrors} is empty
 * except for validation failures.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {
    }
}
