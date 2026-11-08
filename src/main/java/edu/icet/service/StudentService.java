package edu.icet.service;

import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.response.StudentResponse;

import java.util.List;

public interface StudentService {

    void createStudent(StudentCreateRequest request);

    boolean getEmailAndPassword(String email, String password);

    List<StudentResponse> getAll();
}
