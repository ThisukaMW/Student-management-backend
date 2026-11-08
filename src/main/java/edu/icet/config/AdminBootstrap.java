package edu.icet.config;

import edu.icet.entity.Role;
import edu.icet.exception.DuplicateEmailException;
import edu.icet.repository.UserRepository;
import edu.icet.service.UserService;
import edu.icet.validation.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Creates the first ADMIN on startup when none exists, from ADMIN_EMAIL and ADMIN_PASSWORD.
 * Never fails startup: anything missing or invalid is logged as a warning and skipped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
public class AdminBootstrap implements ApplicationRunner {

    private final AdminProperties adminProperties;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (!adminProperties.isConfigured()) {
            log.warn("No ADMIN account exists and ADMIN_EMAIL / ADMIN_PASSWORD are not set, so none was created. "
                    + "Set both in .env (or the environment) and restart to create the first admin.");
            return;
        }
        if (!adminProperties.email().contains("@")) {
            log.warn("ADMIN_EMAIL '{}' is not an email address; the first admin was not created.",
                    adminProperties.email());
            return;
        }
        List<String> problems = PasswordValidator.problems(adminProperties.password());
        if (!problems.isEmpty()) {
            // The rules are logged, never the password itself
            log.warn("ADMIN_PASSWORD does not meet the password rules ({}); the first admin was not created.",
                    String.join(", ", problems));
            return;
        }
        try {
            userService.createUser(adminProperties.email(), adminProperties.password(), Role.ADMIN);
            log.info("Created the first ADMIN account: {}", adminProperties.email().trim().toLowerCase());
        } catch (DuplicateEmailException e) {
            log.warn("ADMIN_EMAIL {} already belongs to a non-admin account; the first admin was not created. "
                    + "Use a different ADMIN_EMAIL.", adminProperties.email());
        }
    }
}
