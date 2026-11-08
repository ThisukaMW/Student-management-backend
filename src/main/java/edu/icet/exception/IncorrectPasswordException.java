package edu.icet.exception;

/**
 * A password-change request that is well-formed but wrong, e.g. the current password doesn't match.
 * Reported as a 400 field error rather than 401, which a client would read as an expired token.
 */
public class IncorrectPasswordException extends FieldValidationException {

    public IncorrectPasswordException(String field, String message) {
        super(field, message);
    }
}
