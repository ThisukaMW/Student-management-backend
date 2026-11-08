package edu.icet.service;

import edu.icet.dto.request.ChangePasswordRequest;
import edu.icet.dto.request.LoginRequest;
import edu.icet.dto.request.StudentCreateRequest;
import edu.icet.dto.response.AuthResponse;
import edu.icet.dto.response.MeResponse;
import edu.icet.dto.response.StudentResponse;

public interface AuthService {

    /** Self-service sign-up: a STUDENT account plus its linked profile. */
    StudentResponse register(StudentCreateRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);

    void logout(String refreshToken, Long userId);

    MeResponse me(Long userId);

    /** Requires the current password; logs out every session on success. */
    void changePassword(Long userId, ChangePasswordRequest request);
}
