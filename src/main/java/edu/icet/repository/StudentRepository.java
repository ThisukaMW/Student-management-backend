package edu.icet.repository;

import edu.icet.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    // Load each student's account in the same query, so the email column doesn't cost one query per row
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Student> findAll(Specification<Student> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<Student> findById(Long id);

    @EntityGraph(attributePaths = "user")
    Optional<Student> findByUserId(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
