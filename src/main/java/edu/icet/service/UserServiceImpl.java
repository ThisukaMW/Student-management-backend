package edu.icet.service;

import edu.icet.entity.Role;
import edu.icet.entity.User;
import edu.icet.exception.DuplicateEmailException;
import edu.icet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User createUser(String email, String rawPassword, Role role) {
        String normalized = EmailNormalizer.normalize(email);
        if (userRepository.existsByEmail(normalized)) {
            throw new DuplicateEmailException(normalized);
        }
        User user = User.builder()
                .email(normalized)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    @Override
    public void changeEmail(User user, String newEmail) {
        String normalized = EmailNormalizer.normalize(newEmail);
        if (normalized.equals(user.getEmail())) {
            return;
        }
        if (userRepository.existsByEmailAndIdNot(normalized, user.getId())) {
            throw new DuplicateEmailException(normalized);
        }
        user.setEmail(normalized);
    }

    @Override
    public void delete(User user) {
        userRepository.delete(user);
    }
}
