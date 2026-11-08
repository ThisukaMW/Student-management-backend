package edu.icet.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidatorTest {

    record Form(@ValidPassword String password) {
    }

    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<String> messages(String password) {
        return validator.validate(new Form(password)).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"secret123", "abcdefg1", "P4ssword with spaces", "pässwört12"})
    void acceptsValidPasswords(String password) {
        assertThat(messages(password)).isEmpty();
    }

    @Test
    void nullIsLeftToNotBlank() {
        assertThat(messages(null)).isEmpty();
    }

    @Test
    void reportsEachBrokenRule() {
        assertThat(messages("short")).containsExactlyInAnyOrder(
                "must be at least 8 characters", "must contain at least one digit");
        assertThat(messages("12345678")).containsExactly("must contain at least one letter");
        assertThat(messages("abcdefgh")).containsExactly("must contain at least one digit");
    }

    @Test
    void limitIsBytesNotCharacters() {
        // 36 two-byte characters + a digit = 37 characters but 73 bytes
        String multiByte = "é".repeat(36) + "1";
        assertThat(multiByte).hasSize(37);
        assertThat(messages(multiByte)).containsExactly("must be at most 72 bytes");

        String exactly72Bytes = "a".repeat(71) + "1";
        assertThat(messages(exactly72Bytes)).isEmpty();
    }
}
