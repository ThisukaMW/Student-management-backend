package edu.icet.service;

import edu.icet.entity.Student;
import edu.icet.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl implements StudentService {
    @Autowired
    StudentRepository studentRepository;

    @Override
    public void createStudent(edu.icet.dto.Student student) {
        Student model = new Student();
        model.setFirstName(student.getFirstName());
        model.setLastName(student.getLastName());
        model.setAge(student.getAge());
        model.setEmail(student.getEmail());
        model.setPassword(student.getPassword());
        model.setInstitute(student.getInstitute());
        model.setBatch(student.getBatch());
        model.setGender(student.getGender());
        studentRepository.save(model);
    }

    @Override
    public boolean getEmailAndPassword(String email, String password) {
        return studentRepository.findStudentByEmailAndPassword(email, password) != null;
    }

    @Override
    public Iterable<Student> getAll() {
        return studentRepository.findAll();
    }
}
