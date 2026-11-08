package edu.icet.mapper;

import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.request.StudentUpdateRequest;
import edu.icet.dto.response.StudentResponse;
import edu.icet.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StudentMapper {

    @Mapping(target = "id", ignore = true)
    Student toEntity(StudentCreateRequest request);

    StudentResponse toResponse(Student student);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntity(StudentUpdateRequest request, @MappingTarget Student student);
}
