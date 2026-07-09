package com.geeknito.LMS_backend.controller;

import com.geeknito.LMS_backend.config.CookieConfig;
import com.geeknito.LMS_backend.dto.LoginRequest;
import com.geeknito.LMS_backend.dto.RegisterRequest;
import com.geeknito.LMS_backend.dto.UserResponse;
import com.geeknito.LMS_backend.response.ApiResponse;
import com.geeknito.LMS_backend.security.JwtService;
import com.geeknito.LMS_backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final CookieConfig cookieConfig;

    @PostMapping("/register/admin")
    public ResponseEntity<ApiResponse> registerAdmin(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.registerAdmin(request);
        return ResponseEntity.ok(new ApiResponse("Admin registered successfully", response));
    }

    @PostMapping("/register/student")
    public ResponseEntity<ApiResponse> registerStudent(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.registerStudent(request);
        return ResponseEntity.ok(new ApiResponse("Student registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        UserResponse user = authService.login(request);
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        
        cookieConfig.createCookie(
                servletResponse,
                token,
                (int) jwtService.getExpirationInSeconds(),
                servletRequest.isSecure()
        );

        return ResponseEntity.ok(new ApiResponse("Login successful", user));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        cookieConfig.clearCookie(servletResponse, servletRequest.isSecure());
        return ResponseEntity.ok(new ApiResponse("Logged out successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(new ApiResponse("Unauthorized", null));
        }
        UserResponse user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(new ApiResponse("Profile retrieved successfully", user));
    }
}
