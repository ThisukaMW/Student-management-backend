package edu.icet.service;

import edu.icet.dto.request.LoginRequest;
import edu.icet.dto.response.StudentResponse;
import edu.icet.exception.InvalidCredentialsException;
import edu.icet.mapper.StudentMapper;
import edu.icet.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    @Override
    public StudentResponse login(LoginRequest request) {
        // Plain-text comparison until passwords are hashed with BCrypt in Phase 2
        return studentRepository.findByEmail(EmailNormalizer.normalize(request.email()))
                .filter(student -> student.getPassword().equals(request.password()))
                .map(studentMapper::toResponse)
                .orElseThrow(InvalidCredentialsException::new);
    }
}
