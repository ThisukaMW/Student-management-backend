package edu.icet.exception;

/** A refresh token that is unknown, expired, revoked or belongs to a disabled account (401). */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("The refresh token is invalid or has expired");
    }

    public InvalidTokenException(String message) {
        super(message);
    }
}
