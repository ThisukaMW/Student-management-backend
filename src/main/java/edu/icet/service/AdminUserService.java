package edu.icet.service;

import edu.icet.dto.request.CreateUserRequest;
import edu.icet.dto.response.UserResponse;

public interface AdminUserService {

    /** Creates a LECTURER or ADMIN account. */
    UserResponse createStaffUser(CreateUserRequest request);
}
