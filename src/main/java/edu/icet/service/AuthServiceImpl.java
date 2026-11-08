package edu.icet.service;

import edu.icet.dto.request.LoginRequest;
import edu.icet.dto.response.StudentResponse;
import edu.icet.entity.User;
import edu.icet.exception.InvalidCredentialsException;
import edu.icet.mapper.StudentMapper;
import edu.icet.repository.StudentRepository;
import edu.icet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public StudentResponse login(LoginRequest request) {
        User user = authenticate(request.email(), request.password());
        return studentRepository.findByUserId(user.getId())
                .map(studentMapper::toResponse)
                .orElseThrow(InvalidCredentialsException::new);
    }

    private User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(EmailNormalizer.normalize(email)).orElse(null);
        if (user == null) {
            // Hash anyway so an unknown email takes as long as a wrong password
            passwordEncoder.matches(rawPassword, dummyHash());
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash()) || !user.isEnabled()) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    // A real hash of a random value, created on first use; only used to equalize timing
    private volatile String dummyHash;

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
        }
        return dummyHash;
    }
}
