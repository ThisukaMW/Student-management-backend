package edu.icet.service;

import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.request.StudentUpdateRequest;
import edu.icet.dto.response.StudentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentService {

    StudentResponse create(StudentCreateRequest request);

    StudentResponse getById(Long id);

    Page<StudentResponse> search(String name, String batch, String institute, Pageable pageable);

    StudentResponse update(Long id, StudentUpdateRequest request);

    void delete(Long id);
}
