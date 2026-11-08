package edu.icet.service;

import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.request.StudentUpdateRequest;
import edu.icet.dto.response.StudentResponse;
import edu.icet.entity.Role;
import edu.icet.entity.Student;
import edu.icet.entity.User;
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
    private final UserService userService;

    @Override
    @Transactional
    public StudentResponse create(StudentCreateRequest request) {
        User user = userService.createUser(request.email(), request.password(), Role.STUDENT);
        Student student = studentMapper.toEntity(request);
        student.setUser(user);
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
        userService.changeEmail(student.getUser(), request.email());
        // Only an ADMIN gets here with these set: @PreAuthorize on the controller rejects owners who send them
        if (request.changesAccountSettings()) {
            userService.updateAccountSettings(student.getUser(), request.role(), request.enabled());
        }
        studentMapper.updateEntity(request, student);
        // Flush now so a unique-constraint race surfaces here, not at commit time
        return studentMapper.toResponse(studentRepository.saveAndFlush(student));
    }

    /** Deletes the profile and its login account: a student account without a profile has no use. */
    @Override
    @Transactional
    public void delete(Long id) {
        Student student = findOrThrow(id);
        studentRepository.delete(student);
        userService.delete(student.getUser());
    }

    private Student findOrThrow(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }
}
