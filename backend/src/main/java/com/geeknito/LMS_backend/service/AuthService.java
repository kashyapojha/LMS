package com.geeknito.LMS_backend.service;

import com.geeknito.LMS_backend.dto.LoginRequest;
import com.geeknito.LMS_backend.dto.RegisterRequest;
import com.geeknito.LMS_backend.dto.UserResponse;

public interface AuthService {
    UserResponse registerAdmin(RegisterRequest request);
    UserResponse registerStudent(RegisterRequest request);
    UserResponse login(LoginRequest request);
    UserResponse getCurrentUser(String email);
}
