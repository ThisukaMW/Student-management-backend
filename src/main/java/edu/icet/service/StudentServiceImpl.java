package edu.icet.service;

import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.request.StudentUpdateRequest;
import edu.icet.dto.response.StudentResponse;
import edu.icet.entity.Student;
import edu.icet.exception.DuplicateEmailException;
import edu.icet.exception.StudentNotFoundException;
import edu.icet.mapper.StudentMapper;
import edu.icet.repository.StudentRepository;
import edu.icet.repository.StudentSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    @Override
    @Transactional
    public StudentResponse create(StudentCreateRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        if (studentRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        Student student = studentMapper.toEntity(request);
        student.setEmail(email);
        return studentMapper.toResponse(studentRepository.save(student));
    }

    @Override
    public StudentResponse getById(Long id) {
        return studentMapper.toResponse(findOrThrow(id));
    }

    @Override
    public Page<StudentResponse> search(String name, String batch, String institute, Pageable pageable) {
        return studentRepository
                .findAll(StudentSpecifications.withFilters(name, batch, institute), pageable)
                .map(studentMapper::toResponse);
    }

    @Override
    @Transactional
    public StudentResponse update(Long id, StudentUpdateRequest request) {
        Student student = findOrThrow(id);
        String email = EmailNormalizer.normalize(request.email());
        if (studentRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateEmailException(email);
        }
        studentMapper.updateEntity(request, student);
        student.setEmail(email);
        // Flush now so a unique-constraint race surfaces here, not at commit time
        return studentMapper.toResponse(studentRepository.saveAndFlush(student));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new StudentNotFoundException(id);
        }
        studentRepository.deleteById(id);
    }

    private Student findOrThrow(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }
}
