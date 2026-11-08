package edu.icet.service;

import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.request.StudentUpdateRequest;
import edu.icet.dto.response.StudentResponse;
import edu.icet.entity.Gender;
import edu.icet.entity.Student;
import edu.icet.exception.DuplicateEmailException;
import edu.icet.exception.StudentNotFoundException;
import edu.icet.mapper.StudentMapperImpl;
import edu.icet.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    private StudentServiceImpl studentService;

    @BeforeEach
    void setUp() {
        // The real generated mapper, so mapping is covered too
        studentService = new StudentServiceImpl(studentRepository, new StudentMapperImpl());
    }

    @Test
    void createSavesStudentWithNormalizedEmailAndReturnsResponse() {
        when(studentRepository.existsByEmail("nimal@example.com")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        StudentResponse response = studentService.create(createRequest("Nimal@Example.COM"));

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        Student saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("nimal@example.com");
        assertThat(saved.getPassword()).isEqualTo("secret123");
        assertThat(saved.getGender()).isEqualTo(Gender.MALE);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("nimal@example.com");
        assertThat(response.firstName()).isEqualTo("Nimal");
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(studentRepository.existsByEmail("nimal@example.com")).thenReturn(true);

        assertThatThrownBy(() -> studentService.create(createRequest("nimal@example.com")))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("nimal@example.com");
        verify(studentRepository, never()).save(any());
    }

    @Test
    void getByIdReturnsStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L, "nimal@example.com")));

        assertThat(studentService.getById(1L).lastName()).isEqualTo("Perera");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getById(99L))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void searchMapsEachStudentToResponse() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Student> page = new PageImpl<>(List.of(student(1L, "nimal@example.com")), pageable, 1);
        when(studentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<StudentResponse> result = studentService.search("nim", null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(StudentResponse::email).containsExactly("nimal@example.com");
    }

    @Test
    void updateReplacesFieldsButKeepsPassword() {
        Student existing = student(1L, "nimal@example.com");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmailAndIdNot("nimal.silva@example.com", 1L)).thenReturn(false);
        when(studentRepository.saveAndFlush(existing)).thenReturn(existing);

        StudentResponse response = studentService.update(1L, updateRequest("Nimal.Silva@example.com"));

        assertThat(response.lastName()).isEqualTo("Silva");
        assertThat(response.batch()).isEqualTo("2024");
        assertThat(response.email()).isEqualTo("nimal.silva@example.com");
        assertThat(existing.getPassword()).isEqualTo("secret123");
        assertThat(existing.getId()).isEqualTo(1L);
    }

    @Test
    void updateThrowsWhenMissing() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.update(99L, updateRequest("a@example.com")))
                .isInstanceOf(StudentNotFoundException.class);
        verify(studentRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateRejectsEmailOwnedByAnotherStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L, "nimal@example.com")));
        when(studentRepository.existsByEmailAndIdNot("kamal@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> studentService.update(1L, updateRequest("kamal@example.com")))
                .isInstanceOf(DuplicateEmailException.class);
        verify(studentRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteRemovesExistingStudent() {
        when(studentRepository.existsById(1L)).thenReturn(true);

        studentService.delete(1L);

        verify(studentRepository).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(studentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> studentService.delete(99L))
                .isInstanceOf(StudentNotFoundException.class);
        verify(studentRepository, never()).deleteById(any());
    }

    private static StudentCreateRequest createRequest(String email) {
        return new StudentCreateRequest("Nimal", "Perera", 21, email, "secret123", "ICET", "2023", Gender.MALE);
    }

    private static StudentUpdateRequest updateRequest(String email) {
        return new StudentUpdateRequest("Nimal", "Silva", 22, email, "ICET", "2024", Gender.MALE);
    }

    static Student student(Long id, String email) {
        return Student.builder()
                .id(id)
                .firstName("Nimal")
                .lastName("Perera")
                .age(21)
                .email(email)
                .password("secret123")
                .institute("ICET")
                .batch("2023")
                .gender(Gender.MALE)
                .build();
    }
}
