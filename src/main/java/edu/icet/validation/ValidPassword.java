package edu.icet.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Password rules: at least 8 characters, at least one letter and one digit, and at most
 * 72 bytes in UTF-8 (BCrypt's input limit). Null is allowed; combine with {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface ValidPassword {

    String message() default "is not a valid password";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
