package edu.icet.service;

import edu.icet.dao.StudentEntity;
import edu.icet.dto.Student;

public interface StudentService {

    void createStudent(Student student);

    boolean getEmailAndPassword(String email,String password);


    Iterable<StudentEntity> getAll();
}
