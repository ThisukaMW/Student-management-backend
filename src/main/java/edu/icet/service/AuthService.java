package edu.icet.service;

import edu.icet.dto.request.LoginRequest;
import edu.icet.dto.response.StudentResponse;

public interface AuthService {

    StudentResponse login(LoginRequest request);
}
