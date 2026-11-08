package edu.icet.exception;

import edu.icet.dto.response.ApiError;
import edu.icet.security.RestAccessDeniedHandler;
import edu.icet.security.RestAuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.List;

/**
 * Turns every exception into an {@link ApiError}. Extending {@link ResponseEntityExceptionHandler}
 * means Spring MVC's own errors (malformed JSON, type mismatch, unknown route, wrong method, ...)
 * use the same shape as ours.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(StudentNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(StudentNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ApiError> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    // Two requests saving the same email at once both pass the service check; the unique constraint catches the second
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "An account with this email already exists", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), List.of());
    }

    // e.g. ?sort=unknownField
    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<ApiError> handleBadSortProperty(PropertyReferenceException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), List.of());
    }

    // @PreAuthorize denials are thrown inside the controller call, so they arrive here rather than at the
    // security filter's AccessDeniedHandler; without this they would fall through to the 500 handler below
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, RestAccessDeniedHandler.FORBIDDEN, request.getRequestURI(), List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, RestAuthenticationEntryPoint.MISSING_TOKEN, request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred",
                request.getRequestURI(), List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        // Rejected values are left out on purpose: they could include a password
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiError.FieldError::field)
                        .thenComparing(ApiError.FieldError::message))
                .toList();
        return asObject(build(HttpStatus.BAD_REQUEST, "Validation failed", path(request), fieldErrors));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        // Spring's ProblemDetail "detail" is written for clients, unlike ex.getMessage()
        String message = body instanceof ProblemDetail problem && problem.getDetail() != null
                ? problem.getDetail()
                : ApiError.reasonPhrase(statusCode);
        ApiError error = ApiError.of(statusCode, message, path(request));
        return ResponseEntity.status(statusCode).headers(headers).body(error);
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String message, String path,
                                                  List<ApiError.FieldError> fieldErrors) {
        return ResponseEntity.status(status).body(ApiError.of(status, message, path, fieldErrors));
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest servlet
                ? servlet.getRequest().getRequestURI()
                : request.getDescription(false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ResponseEntity<Object> asObject(ResponseEntity<ApiError> response) {
        return (ResponseEntity) response;
    }
}
