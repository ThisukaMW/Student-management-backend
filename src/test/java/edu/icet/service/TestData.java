package edu.icet.service;

import edu.icet.entity.Gender;
import edu.icet.entity.Role;
import edu.icet.entity.Student;
import edu.icet.entity.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Shared fixtures for service tests. */
public final class TestData {

    // Low strength keeps tests fast; production uses the default (10)
    public static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    public static final String PASSWORD = "secret123";

    private TestData() {
    }

    public static User user(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .email(email)
                .passwordHash(ENCODER.encode(PASSWORD))
                .role(role)
                .enabled(true)
                .build();
    }

    public static Student student(Long id, User user) {
        return Student.builder()
                .id(id)
                .user(user)
                .firstName("Nimal")
                .lastName("Perera")
                .age(21)
                .institute("ICET")
                .batch("2023")
                .gender(Gender.MALE)
                .build();
    }
}
