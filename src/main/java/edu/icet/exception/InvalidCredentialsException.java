package edu.icet.exception;

/** Thrown for both an unknown email and a wrong password, so callers can't tell which one failed. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
