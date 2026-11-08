package edu.icet.dto.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

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

    public static ApiError of(HttpStatusCode status, String message, String path) {
        return of(status, message, path, List.of());
    }

    public static ApiError of(HttpStatusCode status, String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status.value(), reasonPhrase(status), message, path, fieldErrors);
    }

    public static String reasonPhrase(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.getReasonPhrase() : String.valueOf(status.value());
    }
}
