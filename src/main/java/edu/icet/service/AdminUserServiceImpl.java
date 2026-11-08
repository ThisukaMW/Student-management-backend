package edu.icet.service;

import edu.icet.dto.request.CreateUserRequest;
import edu.icet.dto.response.UserResponse;
import edu.icet.entity.Role;
import edu.icet.exception.FieldValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserService userService;

    @Override
    public UserResponse createStaffUser(CreateUserRequest request) {
        // A STUDENT account without a profile would be useless; students are created with POST /api/v1/students
        if (request.role() == Role.STUDENT) {
            throw new FieldValidationException("role",
                    "must be ADMIN or LECTURER; create students with POST /api/v1/students");
        }
        return UserResponse.from(userService.createUser(request.email(), request.password(), request.role()));
    }
}
