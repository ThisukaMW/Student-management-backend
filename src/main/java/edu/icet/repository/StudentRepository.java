package edu.icet.repository;

import edu.icet.entity.Student;
import org.springframework.data.repository.CrudRepository;

public interface StudentRepository extends CrudRepository<Student, Long> {
    Student findStudentByEmailAndPassword(String email, String password);
}
