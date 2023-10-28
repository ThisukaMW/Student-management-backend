package edu.icet.service;

import edu.icet.dao.StudentEntity;
import edu.icet.dto.Student;
import edu.icet.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl implements StudentService{
    @Autowired
    StudentRepository studentRepository;

    @Override
    public void createStudent(Student student) {
        StudentEntity model = new StudentEntity();
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
        StudentEntity studentByEmailAndPassword = studentRepository.findStudentByEmailAndPassword(email, password);
        return studentByEmailAndPassword!=null ? true : false;
    }

    @Override
    public Iterable<StudentEntity> getAll() {
       return studentRepository.findAll();
    }
}




