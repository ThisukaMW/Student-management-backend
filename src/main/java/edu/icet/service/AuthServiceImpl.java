package edu.icet.service;

import edu.icet.dto.request.ChangePasswordRequest;
import edu.icet.dto.request.LoginRequest;
import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.response.AuthResponse;
import edu.icet.dto.response.MeResponse;
import edu.icet.dto.response.StudentResponse;
import edu.icet.entity.User;
import edu.icet.exception.IncorrectPasswordException;
import edu.icet.exception.InvalidCredentialsException;
import edu.icet.exception.InvalidTokenException;
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
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final PasswordEncoder passwordEncoder;
    private final StudentService studentService;
    private final TokenService tokenService;

    @Override
    public StudentResponse register(StudentCreateRequest request) {
        return studentService.create(request);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        return tokenService.issueTokens(authenticate(request.email(), request.password()));
    }

    // Must match TokenServiceImpl.refresh: rolling back here would undo the revoke-all on token reuse
    @Override
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResponse refresh(String refreshToken) {
        return tokenService.refresh(refreshToken);
    }

    @Override
    public void logout(String refreshToken, Long userId) {
        tokenService.revoke(refreshToken, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        User user = findAccount(userId);
        StudentResponse student = studentRepository.findByUserId(userId).map(studentMapper::toResponse).orElse(null);
        return new MeResponse(user.getId(), user.getEmail(), user.getRole(), user.isEnabled(),
                user.getCreatedAt(), student);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findAccount(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IncorrectPasswordException("currentPassword", "is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IncorrectPasswordException("newPassword", "must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Sessions started with the old password end; the caller's access token runs out within minutes
        tokenService.revokeAll(userId);
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

    // The access token is still valid but its account was deleted
    private User findAccount(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("This account no longer exists"));
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
