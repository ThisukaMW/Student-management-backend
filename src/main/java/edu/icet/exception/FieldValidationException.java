package edu.icet.exception;

import lombok.Getter;

/**
 * A request that passed Bean Validation but breaks a rule only the service can check.
 * Returned as a 400 with a field error, like any other validation failure.
 */
@Getter
public class FieldValidationException extends RuntimeException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
}
