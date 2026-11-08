package edu.icet.service;

import edu.icet.entity.Student;

public interface StudentService {

    void createStudent(edu.icet.dto.Student student);

    boolean getEmailAndPassword(String email, String password);

    Iterable<Student> getAll();
}
