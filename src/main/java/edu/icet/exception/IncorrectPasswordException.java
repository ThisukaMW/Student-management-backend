package edu.icet.exception;

import lombok.Getter;

/**
 * A password-change request that is well-formed but wrong, e.g. the current password doesn't match.
 * Reported as a 400 field error rather than 401, which a client would read as an expired token.
 */
@Getter
public class IncorrectPasswordException extends RuntimeException {

    private final String field;

    public IncorrectPasswordException(String field, String message) {
        super(message);
        this.field = field;
    }
}
