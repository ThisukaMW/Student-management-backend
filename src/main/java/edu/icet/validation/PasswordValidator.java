package edu.icet.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Reports each broken rule as its own message, so the client can show exactly what to fix. */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    public static final int MIN_LENGTH = 8;
    // BCrypt only uses the first 72 bytes and Spring rejects longer input, so cap it here as a 400
    public static final int MAX_BYTES = 72;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true;
        }
        List<String> problems = problems(password);
        if (problems.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        problems.forEach(problem -> context.buildConstraintViolationWithTemplate(problem).addConstraintViolation());
        return false;
    }

    public static List<String> problems(String password) {
        List<String> problems = new ArrayList<>();
        if (password.length() < MIN_LENGTH) {
            problems.add("must be at least " + MIN_LENGTH + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            problems.add("must be at most " + MAX_BYTES + " bytes");
        }
        if (password.chars().noneMatch(Character::isLetter)) {
            problems.add("must contain at least one letter");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            problems.add("must contain at least one digit");
        }
        return problems;
    }
}
