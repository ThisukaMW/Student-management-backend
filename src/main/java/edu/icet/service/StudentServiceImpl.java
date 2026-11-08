package edu.icet.service;

import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.response.StudentResponse;
import edu.icet.mapper.StudentMapper;
import edu.icet.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class StudentServiceImpl implements StudentService {
    @Autowired
    StudentRepository studentRepository;

    @Autowired
    StudentMapper studentMapper;

    @Override
    public void createStudent(StudentCreateRequest request) {
        studentRepository.save(studentMapper.toEntity(request));
    }

    @Override
    public boolean getEmailAndPassword(String email, String password) {
        return studentRepository.findStudentByEmailAndPassword(email, password) != null;
    }

    @Override
    public List<StudentResponse> getAll() {
        return StreamSupport.stream(studentRepository.findAll().spliterator(), false)
                .map(studentMapper::toResponse)
                .toList();
    }
}
